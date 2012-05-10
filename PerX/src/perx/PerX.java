package perx;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import mapservice.MapService;
import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.exception.PeerGroupException;
import net.jxta.exception.ProtocolNotSupportedException;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.PipeAdvertisement;

/**
 *
 * @author Jason
 */
public class PerX implements DiscoveryListener {
   
    static String CONFIG_PATH = "." + System.getProperty("file.separator") + ".jxta";
    static int TCP_PORT = 20012;
    static int HTTP_PORT = 9801;
    static URI SERVER_LOC = URI.create("tcp://192.168.56.1:" + TCP_PORT);       
    //static URI SERVER_LOC = URI.create("tcp://132.236.227.61:" + TCP_PORT); 
    static int doSubmit, showGUI;
    
    private final String userName;
    private NetworkManager manager;
    private PeerGroup netGroup;
    private DiscoveryService discoveryService;
    private MapService map;
    private PipeID localPipe;
    private ConcurrentHashMap<ID, PipeAdvertisement> pipes;
    

    /**
     * @param args doSubmit showGUI
     */
    public static void main(String[] args) throws UnknownHostException {
        showGUI = Integer.parseInt(args[0]);
        Logger.getLogger("net.jxta").setLevel(Level.OFF);
        PerX p = new PerX(Inet4Address.getLocalHost().getHostName());
        p.run();
      
        if (showGUI == 1) {
            PerX_GUI gui = new PerX_GUI(p);
        }
        
    }

    public PerX(String name) {
        this.userName = name;
        try {
            // Preparing the configuration storage location
            File configFile = new File(PerX.CONFIG_PATH);

            // Creation of the network manager
            this.manager = new NetworkManager(
                    NetworkManager.ConfigMode.RENDEZVOUS, this.userName,
                    configFile.toURI());

            //Delete the old configuration for easy debugging
            NetworkManager.RecursiveDelete(configFile);

            NetworkConfigurator myConfigurator = this.manager.getConfigurator();
            myConfigurator.setName(this.userName);
            
            myConfigurator.setTcpPort(PerX.TCP_PORT);
            myConfigurator.setTcpEnabled(true);
            myConfigurator.setTcpIncoming(true);
            myConfigurator.setTcpOutgoing(true);
            myConfigurator.setUseMulticast(false);
            myConfigurator.setPeerID(
                    IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID,
                    this.userName.getBytes()));


            //Delete any old seeds and add the global server seed
            myConfigurator.clearRendezvousSeeds();
            myConfigurator.clearRelaySeeds();
            myConfigurator.addSeedRelay(PerX.SERVER_LOC);
            myConfigurator.addSeedRendezvous(PerX.SERVER_LOC);
            
            myConfigurator.save();

        } catch (IOException ex) {
            Logger.getLogger(PerX.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public PeerGroup getPeerGroup () {
        return this.netGroup;
    }
    
    public MapService getMapService() {
        return this.map;
    }

    private void run() {
        try {
            this.netGroup = this.manager.startNetwork();
            this.discoveryService = this.netGroup.getDiscoveryService();
            this.discoveryService.addDiscoveryListener(this);
            this.map = (MapService) this.netGroup.loadModule(MapService.getModuleClassID(),
                    mapservice.MapService.getModuleImplAdvertisement());
    
            map.startApp(new String[]{this.userName, this.netGroup.getPeerID().toString()});           
        } catch (ProtocolNotSupportedException ex) {
            Logger.getLogger(PerX.class.getName()).log(Level.SEVERE, null, ex);
        } catch (PeerGroupException ex) {
            Logger.getLogger(PerX.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PerX.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void discoveryEvent(DiscoveryEvent event) {
        Enumeration<Advertisement> ads = event.getSearchResults();
        while (ads.hasMoreElements()) {
            Advertisement ad = ads.nextElement();
            if (ad.getAdvType().equals(PipeAdvertisement.getAdvertisementType())) {
                PipeAdvertisement pipeAd = (PipeAdvertisement) ad;
                if (!this.localPipe.equals((PipeID) pipeAd.getPipeID())
                        && !this.pipes.containsKey(pipeAd.getPipeID())) {
                    this.pipes.put(pipeAd.getPipeID(), pipeAd);
                }
            }
        }
    }
}
