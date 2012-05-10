/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mapservice;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jxta.peergroup.PeerGroup;

/**
 *
 * @author Jason
 */
public class WorkRoot implements Runnable {

    static int MAX_SLAVES = 10;
    static int KEY_SPLIT = 2;
    private final File jar, dir, outDir;
    private final String appName;
    private final MapService service;
    private final HashMap<UUID, Object> workQ;
    private final HashMap<UUID, Object> pendingQ;
    private final Set<Object> mapKeys;
    private int pendingCounter, totJobs = 0, jobsDone = 0;
    private byte[] func;
    private boolean mapping;
    private List<String> slaves = null;
    private JobInfo info = null;
    private Map<Object, Object> finalResults;

    public WorkRoot(String app, File jarFile, File dir, MapService service) {
        this.appName = app;
        this.dir = dir;
        this.jar = jarFile;
        this.outDir = new File(dir.getAbsolutePath() + File.separator + "out");
        this.outDir.mkdir();
        this.service = service;
        this.workQ = new HashMap<UUID, Object>();
        this.pendingQ = new HashMap<UUID, Object>();
        this.mapKeys = new HashSet<Object>();
        this.pendingCounter = 0;
        this.mapping = true;
    }

    public JobInfo getJobInfo() {
        return this.info;
    }

    public PeerGroup getPeerGroup() {
        return this.service.getPeerGroup();
    }

    public int getTotalJobs() {
        return this.totJobs;
    }

    public int getJobsDone() {
        return this.jobsDone;
    }

    public List<String> getWorkRemaining() {
        List<String> ret = new LinkedList<String>();
        if (this.mapping) {
            for (Object f : this.workQ.values()) {
                ret.add(((File) f).getName());
            }
        } else {
            for (Object f : this.workQ.keySet()) {
                ret.add(f.toString());
            }
        }

        ret.addAll(getPendingWork());
        return ret;
    }

    public List<String> getPendingWork() {
        List<String> ret = new LinkedList<String>();
        if (this.mapping) {
            for (Object f : this.pendingQ.values()) {
                ret.add(((File) f).getName());
            }
        } else {
            for (Object f : this.pendingQ.keySet()) {
                ret.add(f.toString());
            }
        }
        return ret;
    }

    public Tuple getWork() {
        Tuple newWork = null;
        UUID id = null;
        Object work = null;
        synchronized (this.workQ) {
            if (!this.workQ.isEmpty()) {
                id = this.workQ.keySet().iterator().next();
                work = this.workQ.get(id);
                newWork = new Tuple(id, work);
                this.workQ.remove(id);
            }
        }
        synchronized (this.pendingQ) {
            if (newWork != null) {
                this.pendingQ.put(id, work);
            } else {
                if (!this.pendingQ.isEmpty()) {
                    id = (UUID) this.pendingQ.keySet().toArray()[this.pendingCounter % this.pendingQ.size()];
                    work = this.pendingQ.get(id);
                    newWork = new Tuple(id, work);
                    this.pendingCounter++;
                }
            }
        }
        return newWork;
    }

    public void submitWork(UUID key, byte[] work) {
        synchronized (this.pendingQ) {
            if (!this.pendingQ.containsKey(key)) {
                return;
            } else {
                this.pendingQ.remove(key);
            }
        }
        Object obj = Util.readObject(work);
        if (this.mapping) {
            for (Object e : (Set<Object>) obj) {
                this.mapKeys.add(e);
            }
        } else {
            this.finalResults.putAll((Map<Object, Object>) obj);
        }

        synchronized (this) {
            this.jobsDone++;
            this.notify();
        }
    }

    public boolean isMapping() {
        synchronized (this) {
            return this.mapping;
        }
    }

    @Override
    public void run() {

        try {
            //If file size is greater than max int, need to fix!!!
            this.func = new byte[(int) this.jar.length()];

            FileInputStream stream = new FileInputStream(this.jar);

            stream.read(func);
            stream.close();


        } catch (Exception ex) {
            Logger.getLogger(WorkRoot.class.getName()).log(Level.SEVERE, null, ex);
        }

        synchronized (this.workQ) {
            for (File f : this.dir.listFiles()) {
                if (f.isFile()) {
                    this.workQ.put(UUID.randomUUID(), f);
                    this.totJobs++;
                }
            }
        }

        //Map Phase
        ResourceFinder finder = this.service.getFinder();
        this.slaves = finder.getPeers(MAX_SLAVES);
        this.info = new JobInfo(UUID.randomUUID(), this.appName, this.func, this.slaves);
        List<Scheduler> schedulers = new LinkedList<Scheduler>();
        for (String peer : this.slaves) {
            Scheduler sch = new Scheduler(this, finder.getAdByPeer(peer), peer);
            schedulers.add(sch);
            new Thread(sch).start();
        }

        //Wait until all the work for the map phase is done
        synchronized (this) {
            while (this.jobsDone != this.totJobs) {
                try {
                    this.wait();

                } catch (InterruptedException ex) {
                    Logger.getLogger(WorkRoot.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        //Reduce Phase
        this.finalResults = new HashMap<Object, Object>();
        //Divide up the keys for future work

        int chunk = (int) Math.ceil(this.mapKeys.size() / (this.info.getPeers().size() * KEY_SPLIT));
        int count = 0;
        this.jobsDone = 0;
        this.totJobs = 0;
        this.workQ.clear();
        this.pendingQ.clear();

        synchronized (this) {
            List<Object> keyGroup = new LinkedList<Object>();
            for (Object k : this.mapKeys) {
                if (count < chunk) {
                    keyGroup.add(k);
                    count++;
                } else {
                    this.workQ.put(UUID.randomUUID(), keyGroup);
                    this.totJobs++;
                    keyGroup = new LinkedList<Object>();
                    keyGroup.add(k);
                    count = 1;
                }
            }
            if (count > 0) {
                this.workQ.put(UUID.randomUUID(), keyGroup);
                this.totJobs++;
            }
            //No longer mapping!
            this.mapping = false;
        }


        //Wait until all the work for the map phase is done
        synchronized (this) {
            while (this.jobsDone != this.totJobs) {
                try {
                    this.wait();


                } catch (InterruptedException ex) {
                    Logger.getLogger(WorkRoot.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        for (Scheduler s : schedulers) {
            try {
                s.closeConnection();
            } catch (IOException ex) {
                Logger.getLogger(WorkRoot.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        SortedSet<String> sorted = new TreeSet<String>((Set<String>) ((Set<?>) this.finalResults.keySet()));
        try {
            FileOutputStream outputStream = new FileOutputStream(new File("perx.txt"));
            for (String key : sorted) {
                outputStream.write((key + ":" + this.finalResults.get(key) + "\n").getBytes());

            }
            outputStream.flush();
            outputStream.close();


        } catch (Exception ex) {
            Logger.getLogger(WorkRoot.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println("Job finished!");
    }
    
}
