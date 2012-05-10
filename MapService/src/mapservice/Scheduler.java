/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mapservice;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jxta.endpoint.ByteArrayMessageElement;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.util.JxtaBiDiPipe;

/**
 *
 * @author Jason
 */
public class Scheduler implements Runnable, PipeMsgListener {

    final WorkRoot root;
    JxtaBiDiPipe myPipe;
    WorkState remoteState;
    String remotePeer;
    Tuple work;
    boolean failed, timeout;
    byte[] data;

    public Scheduler(WorkRoot wr, PipeAdvertisement pipeAd, String remotePeer) {
        int retry = 7;
        boolean connected = false;
        this.root = wr;
        this.remoteState = null;
        this.remotePeer = remotePeer;
        while (retry > 0) {
            try {
                myPipe = new JxtaBiDiPipe(this.root.getPeerGroup(), pipeAd, this);
                connected = true;
                break;
            } catch (IOException ex) {
                retry--;
            }
        }
        if (!connected) {
            safe_fail(new SocketTimeoutException());
        }
        System.out.println("[Scheduler] Connected to: " + this.myPipe.getRemotePeerAdvertisement().getName());
    }

    private void handleState() {
        MessageElement element = null;
        switch (this.remoteState) {
            case CLOSING:
                synchronized (this) {
                    this.myPipe = null;
                }
                try {
                    closeConnection();
                } catch (IOException ex) {
                    safe_fail(ex);
                }

                break;
            case WAITING_INFO:
                byte[] output = Util.writeObject(this.root.getJobInfo());
                element = new ByteArrayMessageElement(WorkState.MSG_INFO, null, output, null);
                break;
            case WAITING_DATA:
                synchronized (this) {
                    if (this.data == null) {
                        this.work = this.root.getWork();
                    }
                }
                if (this.data != null) {
                    String type = this.work.getTwo() instanceof File ? WorkState.MSG_MAP : WorkState.MSG_REDUCE;
                    element = new ByteArrayMessageElement(type, null, this.data, null);
                } else if (this.work == null) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Scheduler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    element = new StringMessageElement(WorkState.MSG_STATUS, "", null);
                } else {
                    if (this.root.isMapping()) {
                        //Creating data message element
                        FileInputStream stream;
                        byte[] data_array = null;
                        try {
                            stream = new FileInputStream((File) this.work.getTwo());

                            data_array = new byte[(int) ((File) this.work.getTwo()).length()];
                            stream.read(data_array);

                            stream.close();
                            System.out.println("[Monitor] Sending " + ((File) this.work.getTwo()).getName()
                                    + " to " + this.myPipe.getRemotePeerAdvertisement().getName());
                        } catch (IOException ex) {
                            Logger.getLogger(Scheduler.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        Tuple data_info = new Tuple(this.work.getOne(), data_array);
                        this.data = Util.writeObject(data_info);
                        element = new ByteArrayMessageElement(WorkState.MSG_MAP, null, this.data, null);
                    } else {
                        this.data = Util.writeObject(this.work.getTwo());
                        element = new ByteArrayMessageElement(WorkState.MSG_REDUCE, null, this.data, null);
                        System.out.println("[Monitor] Sending reduce work");
                    }
                }
                break;
            case MAPPING:
                try {
                    //Sleep for 1 second and ask for new status
                    Thread.sleep(1000L);
                } catch (InterruptedException ex) {
                    safe_fail(ex);
                }
            default:
                element = new StringMessageElement(WorkState.MSG_STATUS, "", null);
        }

        if (this.myPipe != null) {
            Message msg = new Message();
            msg.addMessageElement(MapService.NameSpace, element);
            try {
                this.myPipe.sendMessage(msg);
            } catch (IOException ex) {
                safe_fail(ex);
            }
        }
    }

    private void getRemoteState() {
        MessageElement element = new StringMessageElement(WorkState.MSG_STATUS, "", null);
        Message msg = new Message();
        msg.addMessageElement(MapService.NameSpace, element);
        try {
            this.myPipe.sendMessage(msg);
        } catch (IOException ex) {
            safe_fail(ex);
        }
    }

    @Override
    public void run() {
        getRemoteState();
        synchronized (this) {
            try {
                this.wait(2000L);
            } catch (InterruptedException ex) {
                safe_fail(ex);
            }
        }
        if (this.myPipe != null && this.remoteState == null) {
            getRemoteState();
        }
        synchronized (this) {
            while (this.myPipe != null && !this.failed) {
                handleState();
                if (this.myPipe != null) {
                    this.timeout = true;
                    try {
                        this.wait(10000L);
                    } catch (InterruptedException ex) {
                        safe_fail(ex);
                    }
                    if (this.myPipe != null && this.timeout) {
                        safe_fail(new IOException("Processing Timed Out"));
                    }
                }
            }

        }
        System.out.println("[Monitor] Finished!");
    }

    @Override
    public void pipeMsgEvent(PipeMsgEvent event) {
        Message recMsg = event.getMessage();
        Message.ElementIterator messageElements = recMsg.getMessageElements();
        while (messageElements.hasNext()) {
            synchronized (this) {
                this.timeout = false;
            }
            MessageElement ele = messageElements.next();
            if (ele.getElementName().equals(WorkState.MSG_STATUS)) {
                synchronized (this) {
                    this.remoteState = WorkState.fromString(ele.toString());
                    this.notify();
                }
            } else if (this.remoteState == WorkState.MAPPING
                    && ele.getElementName().equals(WorkState.MSG_DATA)) {
                Tuple oldWork = this.work;
                synchronized (this) {
                    this.work = null;
                    this.data = null;
                }
                this.root.submitWork((UUID) oldWork.getOne(), ele.getBytes(false));
            } else if (this.remoteState == WorkState.REDUCING
                    && ele.getElementName().equals(WorkState.MSG_DATA)) {
                Tuple oldWork = this.work;
                synchronized (this) {
                    this.work = null;
                    this.data = null;
                }
                this.root.submitWork((UUID) oldWork.getOne(), ele.getBytes(false));
            }
        }
    }

    public void closeConnection() throws IOException {
        synchronized (this) {
            if (this.myPipe != null) {
                System.out.println("[Monitor] Closing: " + this.myPipe.getRemotePeerAdvertisement().getName());
                Message closeMessage = new Message();
                StringMessageElement ele = new StringMessageElement(WorkState.MSG_CLOSE, "", null);

                closeMessage.addMessageElement(ele);
                this.myPipe.sendMessage(closeMessage);
                this.myPipe.close();
                this.myPipe = null;
            }
        }
    }

    private void safe_fail(Exception ex) {
        this.failed = true;
        try {
            closeConnection();
        } catch (Exception e) {
        }
        if (ex instanceof SocketTimeoutException) {
        } else if (ex instanceof IOException) {
        }       //Logger.getLogger(Scheduler.class.getName()).log(Level.SEVERE, null, ex);
    }
}
