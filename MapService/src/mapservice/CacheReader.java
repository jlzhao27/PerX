/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mapservice;

import java.io.IOException;
import java.util.*;
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
public class CacheReader implements Runnable, PipeMsgListener {

    MapService service;
    UUID job;
    List<String> peers;
    List<Object> keys;
    final Map<Object, List<Object>> results;
    final Queue<Message> messages;
    final Set<String> responses;
    final Set<UUID> ignoreList;
    boolean timeout;
    List<String> globaltimeout;

    public CacheReader(MapService service, UUID job, List<String> peers, List<Object> keys, Set<UUID> ignoreList, Map<Object, List<Object>> output, List<String> timeout) {
        this.service = service;
        this.job = job;
        this.peers = peers;
        this.keys = keys;
        this.results = output;
        this.responses = new HashSet<String>();
        this.messages = new LinkedList<Message>();
        this.ignoreList = ignoreList;
        this.globaltimeout = timeout;
    }

    public void sendRequest(String type, Collection<JxtaBiDiPipe> outPipes) {
        for (JxtaBiDiPipe pipe : outPipes) {
            try {
                Tuple data = new Tuple(this.keys, this.ignoreList);
                Tuple jobTuple = new Tuple(this.job, data);
                byte[] data_byte = Util.writeObject(jobTuple);
                ByteArrayMessageElement element = new ByteArrayMessageElement(type, null, data_byte, null);
                Message msg = new Message();
                msg.addMessageElement(element);
                if (pipe != null) {
                    pipe.sendMessage(msg);
                }

            } catch (IOException ex) {
                //Logger.getLogger(CacheReader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void processMessage() {
        ElementIterator itr = null;
        while (!this.messages.isEmpty()) {
            synchronized (this.messages) {
                Message rec_msg = this.messages.poll();
                itr = rec_msg.getMessageElements();
            }


            while (itr != null && itr.hasNext()) {
                MessageElement rec_ele = itr.next();
                Tuple rec_Tuple = (Tuple) Util.readObject(rec_ele.getBytes(true));
                synchronized (this.responses) {
                    if (!this.responses.contains((String) rec_Tuple.getOne())) {
                        this.responses.add((String) rec_Tuple.getOne());

                    }
                }
                addToResults((Map<Object, List<Object>>) rec_Tuple.getTwo());
            }
        }

    }

    public void addToResults(Map<Object, List<Object>> resultMap) {
        synchronized (this.results) {
            for (Object key : resultMap.keySet()) {
                List<Object> l = this.results.get(key);
                if (l == null) {
                    l = new LinkedList();
                }
                l.addAll((List<Object>) resultMap.get(key));
                this.results.put(key, l);
            }
        }
    }

    public void waitForResponses() {
        //If we dont have all the responses yet, go to sleep for 5 seconds
        while (this.responses.size() < this.peers.size() - 1) {
            synchronized (this.messages) {
                try {
                    this.timeout = true;
                    this.messages.wait(1000L);
                    if (this.timeout) {
                        System.out.println("[Slave] A peer has failed, using backup cache");
                        break;
                    }
                } catch (InterruptedException ex) {
                    //Logger.getLogger(CacheReader.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            processMessage();
        }
    }

    @Override
    public void run() {
        Map<String, JxtaBiDiPipe> outPipes = new HashMap<String, JxtaBiDiPipe>();
        ResourceFinder finder = this.service.getFinder();
        for (String p : this.peers) {
            if (p.equals(this.service.id) || this.service.failedList.contains(p)) {
                continue;
            }
            try {
                System.out.println("Connecting to peer");
                outPipes.put(p, new JxtaBiDiPipe(this.service.getPeerGroup(), finder.getAdByPeer(p), this));
            } catch (IOException ex) {
                //Logger.getLogger(CacheReader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        try {
            Thread.sleep(1000L);
        } catch (InterruptedException ex) {
            //Logger.getLogger(CacheReader.class.getName()).log(Level.SEVERE, null, ex);
        }

        sendRequest(WorkState.MSG_REQ, outPipes.values());

        waitForResponses();

        //Must read the back up
        if (this.responses.size() != this.peers.size() - 1) {
            for (String peer : new LinkedList<String>(this.peers)) {
                if (!this.responses.contains(peer)) {
                    this.service.failedList.add(peer);
                    readBackUpCache(peer, outPipes);
                }
            }
            synchronized (this.messages) {
                try {
                    this.messages.wait(2000L);
                } catch (InterruptedException ex) {
                    Logger.getLogger(CacheReader.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            processMessage();
        }

        synchronized (this.results) {
            System.out.println("notifying parent");
            this.globaltimeout.add("YAY");
            this.results.notify();
        }

        for (JxtaBiDiPipe pipe : outPipes.values()) {
            try {

                StringMessageElement close_ele = new StringMessageElement(WorkState.MSG_CLOSE, "", null);
                Message close_msg = new Message();
                close_msg.addMessageElement(close_ele);
                pipe.sendMessage(close_msg);

            } catch (IOException ex) {
                //Logger.getLogger(CacheReader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void readBackUpCache(String peer, Map<String, JxtaBiDiPipe> pipesMap) {

        List<String> backUp = this.service.getBackUpPeers(peer, this.peers);
        if (backUp.contains(this.service.id)) {
            addToResults(this.service.readLocalCache(true, job, keys));
            this.responses.add(peer);
        } else {
            Collection<JxtaBiDiPipe> pipes = new LinkedList<JxtaBiDiPipe>();
            for (String s : backUp) {
                System.out.println("Reading backup cache from: " + s);
                pipes.add(pipesMap.get(s));
            }
            sendRequest(WorkState.MSG_BKP, pipes);
        }
    }

    @Override
    public void pipeMsgEvent(PipeMsgEvent event) {

        Message msg = event.getMessage();
        synchronized (this.messages) {
            this.messages.add(msg);
            this.timeout = false;
            this.messages.notify();
        }

    }
}
