package perx_server;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashSet;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;

/**
 *
 * @author Jason
 */
public class PerX_Server {

    private final String serverName = "PerX Server v0.1";
    private NetworkManager manager;
    private PeerGroup netGroup;

    private HashSet<PeerID> peers;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws UnknownHostException {
        PerX_Server p = new PerX_Server();
        p.startNetwork();
        //ConnectivityMonitor connectivityMonitor = new ConnectivityMonitor(p.netGroup);
    }

    public PerX_Server() {
        try {
            // Preparing the configuration storage location
            File configFile = new File(Constants.CONFIG_PATH);

            // Creation of the network manager
            this.manager = new NetworkManager(
                    NetworkManager.ConfigMode.RENDEZVOUS_RELAY, this.serverName,
                    configFile.toURI());

            //Delete the old configuration for easy debugging
            NetworkManager.RecursiveDelete(configFile);

            NetworkConfigurator myConfigurator = this.manager.getConfigurator();
            myConfigurator.setName(this.serverName);
            myConfigurator.setTcpPort(Constants.TCP_PORT);
            myConfigurator.setTcpEnabled(true);
            myConfigurator.setTcpIncoming(true);
            myConfigurator.setTcpOutgoing(true);
            
            /*myConfigurator.setHttpPublicAddress("23.21.96.186:9801", true);
            myConfigurator.setHttpEnabled(true);
            myConfigurator.setHttpIncoming(true);
            myConfigurator.setHttpOutgoing(true);
            myConfigurator.setHttpPort(Constants.HTTP_PORT);
                    */
            myConfigurator.setUseMulticast(false);
            myConfigurator.setPeerID(
                    IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID,
                    this.serverName.getBytes()));
            myConfigurator.save();
            
            peers = new HashSet<PeerID>();
        } catch (IOException ex) {
            System.err.println(ex.getStackTrace());
        }
    }

    public void startNetwork() {
        try {
            this.netGroup = this.manager.startNetwork();
        } catch (Exception ex) {
            System.err.println(ex.getStackTrace());
        }
    }

}
