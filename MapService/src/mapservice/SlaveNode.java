/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mapservice;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jxta.endpoint.ByteArrayMessageElement;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.Message.ElementIterator;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.util.JxtaBiDiPipe;

/**
 *
 * @author Jason
 */
public class SlaveNode implements Runnable, PipeMsgListener {

    MapService service;
    JxtaBiDiPipe myPipe;
    String remoteName;
    File tempDir, jarFile, dataFile;
    WorkState state;
    List<Object> reduceWork;
    JobInfo info;
    Message curMsg;
    UUID data_id;

    public SlaveNode(MapService service, JxtaBiDiPipe pipe, String name) {
        this.service = service;
        this.myPipe = pipe;
        this.remoteName = name;
        this.tempDir = new File("CS5412/" + UUID.randomUUID().toString() + "_tmp");
        this.state = WorkState.WAITING_INFO;
    }

    private void sendStatus() {
        Message msg = new Message();
        StringMessageElement ele;
        ele = new StringMessageElement(WorkState.MSG_STATUS, this.state.toString(), null);
        msg.addMessageElement(MapService.NameSpace, ele);
        try {
            synchronized (this) {
                this.myPipe.sendMessage(msg);
            }
        } catch (IOException ex) {
            safe_fail(ex);
        }
    }

    private void processMessage() {
        Message msg;
        synchronized (this) {
            msg = this.curMsg;
            this.curMsg = null;
        }
        ElementIterator elementIterator = msg.getMessageElements();
        while (elementIterator.hasNext()) {
            MessageElement ele = elementIterator.next();
            //The connection needs to be closed
            if (ele.getElementName().equals(WorkState.MSG_CLOSE)) {
                //System.out.println("[Slave] Received Close: " + this.myPipe.getRemotePeerAdvertisement().getName());
                synchronized (this) {
                    this.state = WorkState.CLOSING;
                }
                return;
            }

            if (this.state == WorkState.WAITING_INFO
                    && ele.getElementName().equals(WorkState.MSG_INFO)) {

                this.info = (JobInfo) Util.readObject(ele.getBytes(false));

                //System.out.println(this.info);

                //Load the jarfile from the job information
                try {
                    this.jarFile = new File(this.tempDir + File.separator + "func.jar");
                    this.jarFile.createNewFile();
                    FileOutputStream jarStream = new FileOutputStream(this.jarFile);
                    jarStream.write(this.info.getAppData());
                    jarStream.flush();
                    jarStream.close();
                } catch (IOException ex) {
                    Logger.getLogger(SlaveNode.class.getName()).log(Level.SEVERE, null, ex);
                }



                synchronized (this) {
                    this.state = WorkState.WAITING_DATA;
                }
                //Check if the message element contains data that needs to be processed
            } else if (this.state == WorkState.WAITING_DATA
                    && ele.getElementName().equals(WorkState.MSG_MAP)) {
                Tuple dataTuple = (Tuple) Util.readObject(ele.getBytes(false));
                this.data_id = (UUID) dataTuple.getOne();
                FileOutputStream stream;
                try {
                    this.dataFile = new File(this.tempDir.getPath() + File.separator + "temp.txt");
                    stream = new FileOutputStream(dataFile);
                    stream.write((byte[]) dataTuple.getTwo());
                    stream.flush();
                    stream.close();
                } catch (IOException ex) {
                    Logger.getLogger(SlaveNode.class.getName()).log(Level.SEVERE, null, ex);
                }
                synchronized (this) {
                    this.state = WorkState.MAPPING;
                }
                //Check if the message element contains application class name
            } else if (this.state == WorkState.WAITING_DATA
                    && ele.getElementName().equals(WorkState.MSG_REDUCE)) {

                this.reduceWork = (List<Object>) Util.readObject(ele.getBytes(false));
                synchronized (this) {
                    this.state = WorkState.REDUCING;
                }

            } else if (ele.getElementName().equals(WorkState.MSG_REQ)
                    || ele.getElementName().equals(WorkState.MSG_BKP)) {

                Tuple res = (Tuple) Util.readObject(ele.getBytes(false));
                Map<Object, List<Object>> return_data =
                        this.service.readLocalCache(ele.getElementName().equals(WorkState.MSG_BKP),
                        (UUID) res.getOne(),
                        (List<Object>) ((Tuple) res.getTwo()).getOne(),
                        (Set<UUID>) ((Tuple) res.getTwo()).getTwo());

                Tuple response = new Tuple(this.service.id, return_data);
                byte[] byte_data = Util.writeObject(response);
                Message out_message = new Message();
                ByteArrayMessageElement out_ele = new ByteArrayMessageElement(WorkState.MSG_DATA,
                        null, byte_data, null);

                out_message.addMessageElement(MapService.NameSpace, out_ele);

                try {
                    synchronized (this) {
                        this.myPipe.sendMessage(out_message);
                    }
                } catch (IOException ex) {
                    safe_fail(ex);
                }
                return;

            } else if (ele.getElementName().equals(WorkState.MSG_REP)) {
                Tuple res = (Tuple) Util.readObject(ele.getBytes(false));
                this.service.addtoCache(true,
                        (UUID) res.getOne(),
                        (UUID) ((Tuple) res.getTwo()).getOne(),
                        (Map<Object, List<Object>>) ((Tuple) res.getTwo()).getTwo(),
                        null);
            }
            sendStatus();
        }
    }

    private void runApp() {
        MapInterface mapper;
        try {
            Class mapClass;
            ClassLoader loader = new URLClassLoader(new URL[]{this.jarFile.toURI().toURL()});
            mapClass = loader.loadClass(this.info.getAppName());

            Class<? extends MapInterface> runClass = mapClass.asSubclass(MapInterface.class);
            Constructor<? extends MapInterface> ctor = runClass.getConstructor();

            mapper = ctor.newInstance();


        } catch (Exception ex) {
            safe_fail(ex);
            return;
        }
        byte[] byte_data;
        try {

            if (this.state == WorkState.MAPPING) {
                Map<Object, List<Object>> output = new ConcurrentHashMap<Object, List<Object>>();
                mapper.map(this.dataFile.getName(), this.dataFile, output);
                System.out.println("[Slave] Mapped file from: " + this.myPipe.getRemotePeerAdvertisement().getName());
                //Add to the memory cache
                this.service.addtoCache(false, this.info.jobID, this.data_id, output, this.info.peers);
                this.dataFile.delete();
                byte_data = Util.writeObject(new HashSet<Object>(output.keySet()));


            } else {
                Map<Object, Object> reduce_results = new ConcurrentHashMap<Object, Object>();
                Map<Object, List<Object>> updatedWork = new HashMap<Object, List<Object>>();
                List<String> timeout = new LinkedList<String>();
                int do_wait = this.service.readRemoteCache(this.info.jobID, this.reduceWork, this.info.peers, updatedWork, timeout);
                if (do_wait == 1) {
                    synchronized (updatedWork) {
                        while (timeout.isEmpty()) {
                            System.out.println("Wait");
                            updatedWork.wait(2000L);

                            if (!timeout.isEmpty()) {
                                break;
                            }
                                sendStatus();
                            
                        }
                    }
                } else {
                    updatedWork = null;
                }
                Map<Object, List<Object>> localWork = this.service.readLocalCache(false, this.info.jobID, this.reduceWork);


                for (Object key : this.reduceWork) {
                    List<Object> combined_results = new LinkedList<Object>();

                    if (updatedWork != null) {
                        List<Object> temp = updatedWork.get(key);
                        if (temp != null) {
                            combined_results.addAll(temp);
                        }
                    }

                    if (localWork != null) {
                        List<Object> temp = localWork.get(key);
                        if (temp != null) {
                            combined_results.addAll(temp);
                        }
                    }

                    mapper.reduce(key, combined_results, reduce_results);
                }
                byte_data = Util.writeObject(reduce_results);
                System.out.println("Reduced results!");
            }

            Message message = new Message();
            ByteArrayMessageElement ele = new ByteArrayMessageElement(WorkState.MSG_DATA,
                    null, byte_data, null);

            message.addMessageElement(MapService.NameSpace, ele);

            synchronized (this) {
                if (this.myPipe != null) {
                    this.myPipe.sendMessage(message);
                }
            }
        } catch (Exception ex) {
            safe_fail(ex);
        }
    }

    @Override
    public void run() {
        this.myPipe.setMessageListener(this);
        this.tempDir.mkdirs();
        while (this.myPipe != null) {
            //Wait for new command from the remote
            synchronized (this) {
                try {
                    if (this.curMsg == null) {
                        this.wait(15000L);
                        if (this.curMsg == null) {
                            safe_fail(new SocketTimeoutException());
                        }
                    }
                } catch (InterruptedException ex) {
                    safe_fail(ex);
                }
            }
            if (this.myPipe != null) {
                processMessage();
            }
            //Mapping
            if (this.state == WorkState.MAPPING || this.state == WorkState.REDUCING) {
                runApp();
                synchronized (this) {
                    this.state = WorkState.WAITING_DATA;
                }
            } else if (this.state == WorkState.CLOSING) {
                try {
                    closeConnection();
                } catch (Exception e) {
                    safe_fail(e);
                }
            }
        }
        //System.out.println("[Slave] finished!");
    }

    @Override
    public void pipeMsgEvent(PipeMsgEvent event) {
        synchronized (this) {
            this.curMsg = event.getMessage();
            this.notify();
        }
    }

    private void closeConnection() throws IOException {
        //System.out.println("[Slave] Closing: " + this.remoteName);
        this.service.removeRemote(this.remoteName);
        synchronized (this) {
            if (this.myPipe != null) {
                this.myPipe.close();
            }
            this.myPipe = null;
        }
        recursiveDelete(this.tempDir);

    }

    private void recursiveDelete(File dir) {
        if (dir.isDirectory()) {
            for (File f : dir.listFiles()) {
                recursiveDelete(f);
            }
        }
        dir.delete();
    }

    //Cleans up the temporary directory and closes the connection
    private void safe_fail(Exception e) {
        try {
            closeConnection();
        } catch (Exception ex) {
            //Logger.getLogger(SlaveNode.class.getName()).log(Level.SEVERE, null, ex);
        }
        //Logger.getLogger(SlaveNode.class.getName()).log(Level.SEVERE, null, e);
    }
}
