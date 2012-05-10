/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mapservice;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map.Entry;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.endpoint.ByteArrayMessageElement;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.impl.peergroup.StdPeerGroup;
import net.jxta.impl.protocol.ModuleImplAdv;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeService;
import net.jxta.platform.ModuleClassID;
import net.jxta.platform.ModuleSpecID;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.ModuleSpecAdvertisement;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.service.Service;
import net.jxta.util.JxtaBiDiPipe;
import net.jxta.util.JxtaServerPipe;
import sun.misc.UUDecoder;
import sun.nio.cs.ext.TIS_620;

/**
 *
 * @author Jason
 */
public class MapService implements Service, Runnable {

    public static final int REPLICATION_FACTOR = 1;
    //Static class variables
    public static final String Name = "Map Service";
    public static final String NameSpace = "MapService";
    public static final String BaseModuleClassIDString = "urn:jxta:uuid-14A140993D534B1CA8B0ECAA21F71FA305";
    public static final String BaseModuleSpecIDString =
            "urn:jxta:uuid-14A140993D534B1CA8B0ECAA21F71FA3A7D4113A19484F18B74BE064F9C1455F06";
    public static ModuleClassID BaseModuleClassID = null;
    public static ModuleSpecID BaseModuleSpecID = null;

    static {
        try {
            BaseModuleClassID = ModuleClassID.create(new URI(BaseModuleClassIDString));
            BaseModuleSpecID = ModuleSpecID.create(new URI(BaseModuleSpecIDString));
        } catch (URISyntaxException ex) {
            Logger.getLogger(MapService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    //Non-static variables
    private PeerGroup peerGroup;
    private ModuleClassID moduleClassID;
    private ModuleSpecID moduleSpecID;
    private ModuleImplAdv implAdv;
    String userName, id;
    private List<WorkRoot> jobsToDo;
    private List<String> remoteJobs;
    private ResourceFinder finder;
    private ModuleSpecAdvertisement modSpecAd;
    private JxtaServerPipe serverPipe;
    //Job id, data id, key, value
    private Map<UUID, Map<UUID, Map<Object, List<Object>>>> memCached, back_memCached;
    public Set<String> failedList;

    public static ModuleClassID getModuleClassID() {

        return IDFactory.newModuleClassID(MapService.BaseModuleClassID);
    }

    public static ModuleImplAdvertisement getModuleImplAdvertisement() {
        ModuleImplAdvertisement advertisement = (ModuleImplAdvertisement) AdvertisementFactory.newAdvertisement(ModuleImplAdvertisement.getAdvertisementType());

        //Setting parameters
        advertisement.setDescription(MapService.NameSpace);
        advertisement.setModuleSpecID(BaseModuleSpecID);
        advertisement.setProvider(MapService.Name);
        advertisement.setCode(MapService.class.getName());

        //Setting compatibility and binding
        advertisement.setCompat(StdPeerGroup.STD_COMPAT);

        //This line is not completely correct
        advertisement.setUri("file:" + System.getProperty("user.dir") + File.separator + "lib/MapService.jar");

        return advertisement;
    }

    private PipeAdvertisement getPipeAdvertisement() {
        PipeAdvertisement ad = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        PipeID loc_id = IDFactory.newPipeID(this.peerGroup.getPeerGroupID(), this.userName.getBytes());

        ad.setPipeID(loc_id);
        ad.setType(PipeService.UnicastType);
        ad.setName(MapService.NameSpace);
        ad.setDescription(this.id);

        return ad;
    }

    public ModuleSpecAdvertisement getModuleSpecAdvertisement() {
        ModuleSpecAdvertisement advertisement = (ModuleSpecAdvertisement) AdvertisementFactory.newAdvertisement(ModuleSpecAdvertisement.getAdvertisementType());

        advertisement.setName(MapService.NameSpace + ":" + this.userName);
        advertisement.setCreator(this.userName);
        advertisement.setDescription(this.id);
        advertisement.setModuleSpecID(this.moduleSpecID);
        advertisement.setVersion("1.0");
        advertisement.setPipeAdvertisement(getPipeAdvertisement());

        return advertisement;
    }

    @Override
    public Service getInterface() {
        return this;
    }

    @Override
    public Advertisement getImplAdvertisement() {
        return this.implAdv;
    }

    @Override
    public void init(PeerGroup group, ID assignedID, Advertisement implAdv) throws PeerGroupException {
        this.peerGroup = group;
        this.moduleClassID = (ModuleClassID) assignedID;
        this.moduleSpecID = IDFactory.newModuleSpecID(this.moduleClassID);
        this.implAdv = (ModuleImplAdv) implAdv;
    }

    @Override
    public int startApp(String[] args) {
        this.userName = args[0];
        this.id = args[1];
        this.jobsToDo = new LinkedList<WorkRoot>();
        this.remoteJobs = new LinkedList<String>();
        this.finder = new ResourceFinder(this.peerGroup);
        this.memCached = new ConcurrentHashMap<UUID, Map<UUID, Map<Object, List<Object>>>>();
        this.back_memCached = new ConcurrentHashMap<UUID, Map<UUID, Map<Object, List<Object>>>>();
        this.failedList = new ConcurrentSkipListSet<String>();
        Thread t = new Thread(this);
        t.start();

        return 0;
    }

    @Override
    public void stopApp() {
        System.out.println("Stopped Map Service!");
    }

    public void submitWork() {
        System.out.println("Starting MapReduce");
        try {

            //Retriving the location of the jar file
           /*
             * JFileChooser jarFileChooser = new JFileChooser();
             *
             *
             * if (jarFileChooser.showOpenDialog(null) ==
             * JFileChooser.APPROVE_OPTION) { jarFile =
             * jarFileChooser.getSelectedFile(); }
             */

            //Load the runnable jar file of the map function
            File jarFile = new File(System.getProperty("user.home")
                    + "/Documents/CS5412/WordCount/dist/WordCount.jar");

            //Get the directory of the input files
            File dir = new File(System.getProperty("user.home") + "/Documents/CS5412/txt");

            WorkRoot root = new WorkRoot("wordcount.MapWord", jarFile, dir, this);
            this.jobsToDo.add(root);
            Thread work = new Thread(root);

            work.start();

        } catch (SecurityException ex) {
            Logger.getLogger(MapService.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public List<String> getBackUpPeers(String peer, List<String> peer_list) {
        if (REPLICATION_FACTOR == peer_list.size()) {
            return null;
        }
        List<String> back_up = new LinkedList<String>();
        int index = peer_list.indexOf(peer);
        for (int i = index + 1; i < index + 1 + REPLICATION_FACTOR; ++i) {
            back_up.add(peer_list.get(i % peer_list.size()));
        }
        return back_up;
    }

    public PeerGroup getPeerGroup() {
        return this.peerGroup;
    }

    public ResourceFinder getFinder() {
        return this.finder;
    }

    public List<WorkRoot> getLocalJobs() {
        return this.jobsToDo;
    }

    public List<String> getRemoteJobs() {
        return this.remoteJobs;
    }

    public void removeRemote(String name) {
        synchronized (this) {
            this.remoteJobs.remove(name);
        }
    }

    public synchronized void addtoCache(boolean use_backup, UUID jobId, UUID data_id, Map<Object, List<Object>> items, List<String> peers) {
        Map<UUID, Map<UUID, Map<Object, List<Object>>>> target = use_backup ? this.back_memCached : this.memCached;
        Map<UUID, Map<Object, List<Object>>> cache;
        Map<Object, List<Object>> data_cache;
        if (target.containsKey(jobId)) {
            cache = target.get(jobId);
        } else {
            cache = new ConcurrentHashMap<UUID, Map<Object, List<Object>>>();
        }

        if (cache.containsKey(data_id)) {
            return;
        } else {
            data_cache = new ConcurrentHashMap<Object, List<Object>>();
        }


        for (Entry<Object, List<Object>> e : items.entrySet()) {
            List<Object> curItem = e.getValue();
            data_cache.put(e.getKey(), curItem);
        }

        cache.put(data_id, data_cache);
        target.put(jobId, cache);

        if (!use_backup) {
            new Thread(new Replicator(this, jobId, data_id, items, getBackUpPeers(this.id, peers))).start();
        }
    }

    public synchronized Map<Object, List<Object>> readLocalCache(boolean backup, UUID job, List<Object> keys) {
        return readLocalCache(backup, job, keys, null);
    }

    public synchronized Map<Object, List<Object>> readLocalCache(boolean backup, UUID job, List<Object> keys, Set<UUID> ignoreList) {
        Map<UUID, Map<Object, List<Object>>> job_map = backup ? this.back_memCached.get(job) : this.memCached.get(job);
        Map<Object, List<Object>> output = new ConcurrentHashMap<Object, List<Object>>();

        if (job_map != null) {
            for (UUID data_id : job_map.keySet()) {
                if (ignoreList == null || !ignoreList.contains(data_id)) {
                    Map<Object, List<Object>> local_cache = job_map.get(data_id);
                    for (Object k : keys) {
                        if (local_cache.containsKey(k)) {
                            List<Object> curList = output.get(k);
                            if (curList == null) {
                                curList = local_cache.get(k);
                            } else {
                                curList.addAll(local_cache.get(k));
                            }
                            output.put(k, curList);
                        }
                    }
                }
            }
        }
        return output;
    }

    public int readRemoteCache(UUID job, List<Object> keys, List<String> peers, Map<Object, List<Object>> output, List<String> timeout) {
        if (peers.size() == 1) {
            return 0;
        } else if (!this.memCached.containsKey(job)) {
            return 0;
        }else {
            Set<UUID> ignoreList;
            ignoreList = new HashSet<UUID>(this.memCached.get(job).keySet());
            CacheReader reader = new CacheReader(this, job, peers, keys, ignoreList, output, timeout);
            synchronized (reader) {
                new Thread(reader).start();
            }
        }
        return 1;
    }

    @Override
    public void run() {
        Thread f = new Thread(this.finder);
        f.start();

        try {
            //Generates a new server pipe and listens for incoming work
            this.modSpecAd = this.getModuleSpecAdvertisement();
            this.serverPipe = new JxtaServerPipe(this.peerGroup, this.modSpecAd.getPipeAdvertisement());

            //publish an advertisement that there is a Map Service running on this machine
            this.peerGroup.getDiscoveryService().publish(this.modSpecAd);
            this.peerGroup.getDiscoveryService().remotePublish(this.modSpecAd);
            while (this.serverPipe != null) {
                try {
                    JxtaBiDiPipe biDiPipe = this.serverPipe.accept();
                    synchronized (this.remoteJobs) {
                        this.remoteJobs.add(biDiPipe.getRemotePeerAdvertisement().getName());
                    }
                    Thread slave = new Thread(new SlaveNode(this, biDiPipe,
                            biDiPipe.getRemotePeerAdvertisement().getName()));
                    slave.start();
                    
                } catch (SocketTimeoutException ex) {
                }
            }

        } catch (IOException ex) {
            Logger.getLogger(MapService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
