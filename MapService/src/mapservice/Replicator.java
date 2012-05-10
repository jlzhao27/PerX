/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mapservice;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jxta.endpoint.ByteArrayMessageElement;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.util.JxtaBiDiPipe;

/**
 *
 * @author Jason
 */
public class Replicator implements Runnable {
    MapService service;
    UUID job, data;
    Map<Object, List<Object>> items;
    List<String> peers;
    
    public Replicator(MapService service, UUID jobId, UUID data_id, Map<Object, List<Object>> items, List<String> peers) {
        this.service = service;
        this.job = jobId;
        this.data = data_id;
        this.items = items;
        this.peers = peers;
    }
    
    public void run() {
        
     System.out.println("Replicating to: " + peers);
        if (peers == null) {
            return;
        }
        List<JxtaBiDiPipe> outPipes = new LinkedList<JxtaBiDiPipe>();
        for (String p : peers) {
            try {
                outPipes.add(new JxtaBiDiPipe(this.service.getPeerGroup(), this.service.getFinder().getAdByPeer(p), null));
            } catch (IOException ex) {
                //Logger.getLogger(MapService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        for (JxtaBiDiPipe pipe : outPipes) {
            try {
                Tuple data = new Tuple(this.data, items);
                Tuple jobTuple = new Tuple(this.job, data);
                byte[] data_byte = Util.writeObject(jobTuple);
                ByteArrayMessageElement element = new ByteArrayMessageElement(WorkState.MSG_REP, null, data_byte, null);
                Message msg = new Message();
                msg.addMessageElement(element);
                pipe.sendMessage(msg);
            } catch (IOException ex) {
                //Logger.getLogger(MapService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        for (JxtaBiDiPipe pipe : outPipes) {
            try {
                StringMessageElement close_ele = new StringMessageElement(WorkState.MSG_CLOSE, "", null);
                Message close_msg = new Message();
                close_msg.addMessageElement(close_ele);
                pipe.sendMessage(close_msg);

            } catch (IOException ex) {
                Logger.getLogger(CacheReader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
