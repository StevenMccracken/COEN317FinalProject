package edu.scu.kademlia;

import java.util.ArrayList;

public class Bucket {
    // k-bucket k size
    private int ksize;

    // The bucket id
    private int bucketID;

    // The k-bucket associated host
    private Host localHost;

    private ArrayList<Host> nodesInBucket;
    private ArrayList<Host> nodesBackup;


    public Bucket(int ksize, int bucketID, Host localNode){
        this.ksize = ksize;
        this.bucketID = bucketID;
        this.localHost = localNode;
        this.nodesInBucket = new ArrayList<Host>(ksize);
        this.nodesBackup = new ArrayList<Host>();
    }

    public int getBucketID(){
        return this.bucketID;
    }

    /**
     * move a node to the tail of its bucket
     * @param nodesInBucket, the node to be added into the bucket
     */
    private void moveToLast(ArrayList<Host> nodesInBucket, Host node){
        nodesInBucket.remove(node);
        nodesInBucket.add(node);
    }

    /**
     * Get least recent seen node in a bucket
     * @param  nodesInBucket all node in this bucket
     * @return least recent seen node
     */
    private Host getLeastRecentSeen(ArrayList<Host> nodesInBucket) {
        Host resHost = nodesInBucket.get(0);
        long maxTime=0;
        long interval = 0;

        for(Host curHost: nodesInBucket){
            interval = curHost.getLastSeenTime() - System.currentTimeMillis();
            if(interval > maxTime){
                maxTime = interval;
                resHost = curHost;
            }
        }
        return resHost;
    }

    public void addNodeToBucket(Host node, KademliaRPC rpc){
        //case 1: bucket not full and bucket not contain this node
        if(nodesInBucket.size() < ksize && !nodesInBucket.contains(node)){
            System.out.println("case 1: before size: "+nodesInBucket.size()); //for test only
            //append node to the tail
            nodesInBucket.add(node);
            System.out.println("case 1: after size: "+nodesInBucket.size()); //for test only
        }

        //case2: bucket not full and bucket contains this node
        else if(nodesInBucket.size() <= ksize && nodesInBucket.contains(node)){
            System.out.println("case 2: before size: "+nodesInBucket.size()); //for test only
            moveToLast(nodesInBucket, node);
            System.out.println("case 2: after size: "+nodesInBucket.size()); //for test only
            System.out.println("same node moved to last");
        }

        //bucket full: ping least recent seen node in the bucket
        // case3_1: if not reply, remove least recent seen node and add this node to tail
        // case3_2: if reply, move this least recent seen node to tail. add new node as backup.
        else{
            Host pingNode = getLeastRecentSeen(nodesInBucket);
            //Case 3_1:
            if(rpc.pingNode(node) == false) {
                System.out.println("case 3_1: before size: "+nodesInBucket.size()); //for test only
                nodesInBucket.remove(pingNode);
                System.out.println("case 3_1: after remove size: "+nodesInBucket.size()); //for test only
                nodesInBucket.add(node);
                System.out.println("case 3_1: after add size: "+nodesInBucket.size()); //for test only
            }
            //case 3_2:
            else{
                moveToLast(nodesInBucket, pingNode);
                System.out.println("case 3_2: before nodesbackup size: "+nodesBackup.size()); //for test only
                nodesBackup.add(node);
                System.out.println("case 3_2: after nodesbackup size: "+ nodesBackup.size()); //for test only
                System.out.println("can not add this node, all nodes in the bucket are alive!");
            }
        }
    }


    /**
     * update the network by refresh the k-buckets
     */
    public void BucketRefreshing(KademliaRPC rpc){
        System.out.println("start step1");
        ArrayList<Host> copyNodesInBucket = new ArrayList<>(nodesInBucket);
        for (Host curHost: copyNodesInBucket) {
            //RPC ping curHost, if cur not reply, else keep cur (no action performed)
            if(!rpc.pingNode(curHost)) {

                System.out.println("step1: before nodesinbucket size: "+nodesInBucket.size()); //for test only
                nodesInBucket.remove(curHost);
                System.out.println("step1: after nodesinbucket size: "+nodesInBucket.size()); //for test only
            }
        }

        System.out.println("start step2");
        //Step2: refill nodes(from backup) to the bucket
        while(nodesInBucket.size() < ksize && !nodesBackup.isEmpty()){
            //check this cur host is still alive. if cur is alive, add cur to bucket. Otherwise continue.
            Host curBackup = nodesBackup.get(0);

            if(rpc.pingNode(curBackup)){

                System.out.println("step2 before nodebackup size: "+nodesBackup.size()); //for test only
                nodesBackup.remove(curBackup);
                System.out.println("step2 after nodesbackup size: "+nodesBackup.size()); //for test only
                nodesInBucket.add(curBackup);
                System.out.println("step2: after nodesinbucket size: "+nodesInBucket.size()); //for test only
            }

        }
    }

}



