package edu.scu.kademlia;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

public class Bucket {
    // k-bucket k size
    private int ksize;

    @Getter
    private ArrayList<Host> nodesInBucket;

        //    private ArrayList<Host> nodesBackup;
    private KademliaRPC rpc;

    @Getter
    private UUID uuid;

    public Bucket(int ksize, KademliaRPC rpc) {
        this.uuid = UUID.randomUUID();
        this.ksize = ksize;
        this.nodesInBucket = new ArrayList<Host>(ksize);
//        this.nodesBackup = new ArrayList<Host>();
        this.rpc = rpc;
    }

    public void removeHost(Host host) {
        nodesInBucket.remove(host);
    }

    /**
     * move a node to the tail of its bucket
     */
    private void moveToLast(Host node){
        nodesInBucket.remove(node);
        nodesInBucket.add(node);
    }

    /**
     * Get least recent seen node in a bucket
     * @return least recent seen node
     */
    private Host getLeastRecentSeen() {
        Host resHost = nodesInBucket.get(0);
        long maxTime=0;
        long interval = 0;

        for(Host curHost: nodesInBucket){
            interval = curHost.getMostRecentSeen() - System.currentTimeMillis();
            if(interval > maxTime){
                maxTime = interval;
                resHost = curHost;
            }
        }
        return resHost;
    }

    public boolean addHost(Host host){
        //case 1: bucket not full and bucket not contain this node
        if(nodesInBucket.size() < ksize && !nodesInBucket.contains(host)){
//            System.out.println("case 1: before size: "+nodesInBucket.size()); //for test only
            //append node to the tail
            nodesInBucket.add(host);
//            System.out.println("case 1: after size: "+nodesInBucket.size()); //for test only
            return true;
        }

        //case2: bucket not full and bucket contains this node
        if(nodesInBucket.size() <= ksize && nodesInBucket.contains(host)){
//            System.out.println("case 2: before size: "+nodesInBucket.size()); //for test only
            moveToLast(host);
//            System.out.println("case 2: after size: "+nodesInBucket.size()); //for test only
            return true;
        }

        //bucket full: ping least recent seen node in the bucket
        // case3_1: if not reply, remove least recent seen node and add this node to tail
        // case3_2: if reply, move this least recent seen node to tail. add new node as backup.
        Host pingNode = getLeastRecentSeen();
        //Case 3_1:
        if(!rpc.ping(host)) {
//            System.out.println("case 3_1: before size: "+nodesInBucket.size()); //for test only
            nodesInBucket.remove(pingNode);
//            System.out.println("case 3_1: after remove size: "+nodesInBucket.size()); //for test only
            nodesInBucket.add(host);
//            System.out.println("case 3_1: after add size: "+nodesInBucket.size()); //for test only
            return true;
        }

        return false;
        //case 3_2:
//        else{
//            moveToLast(nodesInBucket, pingNode);
//            System.out.println("case 3_2: before nodesbackup size: "+nodesBackup.size()); //for test only
//            nodesBackup.add(node);
//            System.out.println("case 3_2: after nodesbackup size: "+ nodesBackup.size()); //for test only
//            System.out.println("can not add this node, all nodes in the bucket are alive!");
//        }
    }


    /**
     * update the network by refresh the k-buckets
     */
    public void refreshBucket() {
        //step 1: rule out not respond hosts in the bucket
        nodesInBucket.removeIf(curHost-> !rpc.ping(curHost));

//        //Step2: refill nodes(from backup) to the bucket
//        System.out.println("start step2");
//        while(nodesInBucket.size() < ksize && !nodesBackup.isEmpty()){
//            //check this cur host is still alive. if cur is alive, add cur to bucket. Otherwise continue.
//            Host curBackup = nodesBackup.get(0);
//
//            if(rpc.ping(curBackup)){
//
//                System.out.println("step2 before nodebackup size: "+nodesBackup.size()); //for test only
//                nodesBackup.remove(curBackup);
//                System.out.println("step2 after nodesbackup size: "+nodesBackup.size()); //for test only
//                nodesInBucket.add(curBackup);
//                System.out.println("step2: after nodesinbucket size: "+nodesInBucket.size()); //for test only
//            }
//
//        }
    }

    public boolean contains(Host host) {
        return nodesInBucket.contains(host);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bucket bucket = (Bucket) o;
        return Objects.equals(uuid, bucket.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

}



