/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mapservice;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author Jason
 */
public class JobInfo implements Serializable {
    
    UUID jobID;
    String appName;
    byte[] appData;
    List<String> peers;
    
    public JobInfo(UUID job, String name, byte[] data, List<String> peers) {
        this.jobID = job;
        this.appName = name;
        this.appData = data;
        this.peers = peers;
    }

    public byte[] getAppData() {
        return appData;
    }

    public String getAppName() {
        return appName;
    }

    public UUID getJobID() {
        return jobID;
    }

    public List<String> getPeers() {
        return peers;
    }
    
    @Override
    public String toString() {
        return this.jobID.toString() + "\n" + this.appName + "\n" + this.peers;
    }
}
