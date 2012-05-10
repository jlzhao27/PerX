/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mapservice;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.impl.protocol.ModuleSpecAdv;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.protocol.PipeAdvertisement;

/**
 *
 * @author Jason
 */
public class ResourceFinder implements DiscoveryListener, Runnable {

    final PeerGroup group;
    final String ownId;
    final HashMap<String, PipeAdvertisement> knownPeers;

    public ResourceFinder(PeerGroup group) {
        this.group = group;
        this.ownId = this.group.getPeerID().toString();
        this.knownPeers = new HashMap<String, PipeAdvertisement>();
    }

    public List<String> getPeers(int limit) {
        int count = 0;
        List<String> output = new ArrayList<String>();
        synchronized (this.knownPeers) {
            for (String peer : this.knownPeers.keySet()) {
                output.add(peer);
                count++;
                if (count == limit) {
                    break;
                }
            }
        }
        return output;
    }
    
    public PipeAdvertisement getAdByPeer(String peerID) {
        return this.knownPeers.get(peerID);
    }

    @Override
    public void discoveryEvent(DiscoveryEvent event) {
        Enumeration<Advertisement> searchResults = event.getSearchResults();
        while (searchResults.hasMoreElements()) {
            ModuleSpecAdv ad = (ModuleSpecAdv) searchResults.nextElement();
            synchronized (this.knownPeers) {
                if (!ad.getDescription().equals(this.ownId) && !this.knownPeers.containsKey(ad.getDescription())) {
                    this.knownPeers.put(ad.getDescription(), ad.getPipeAdvertisement());
                }
            }
        }

    }

    @Override
    public void run() {
        DiscoveryService dis = this.group.getDiscoveryService();
        dis.addDiscoveryListener(this);
        while (true) {
            for (PeerID p : this.group.getRendezVousService().getLocalRendezVousView()) {
                dis.getRemoteAdvertisements(p.toString(), DiscoveryService.ADV,
                        "Name", MapService.NameSpace + "*", 50);
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                Logger.getLogger(ResourceFinder.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
