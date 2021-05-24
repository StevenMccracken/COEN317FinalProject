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

    /**
     * move a node to the tail of its bucket
     * @param nodesInBucket, the node to be added into the bucket
     */
    private void moveToLast(ArrayList nodesInBucket, Host node){
        int movePosition = nodesInBucket.indexOf(node);
        for(int i=movePosition; i < nodesInBucket.size()-1; i++){
            nodesInBucket.set(i, nodesInBucket.get(i+1));
        }
        nodesInBucket.set(nodesInBucket.size()-1, node);
    }

    /**
     * Get least recent seen node in a bucket
     * @param  nodesInBucket all node in this bucket
     * @return least recent seen node
     */
    private Host getLeastRecentSeen(ArrayList nodesInBucket) {
        Host cur, resNode;
        int index = 0;
        long maxTime=0;
        long interval = 0;

        for (int i = 0; i < nodesInBucket.size(); i++) {
            cur = (Host)nodesInBucket.get(i);
            interval = cur.getLastSeenTime() - System.currentTimeMillis();

            if(interval > maxTime){
                maxTime = interval;
                index = nodesInBucket.indexOf(cur);
            }
        }
        resNode = (Host) nodesInBucket.get(index);
        return resNode;
    }

    public void addNodeToBucket(Host node){
        //bucket not full and bucket not contain this node
        if(nodesInBucket.size() < ksize && !nodesInBucket.contains(node)){
            //append node to the tail
            nodesInBucket.add(node);

        }
        //bucket not full and bucket contains this node
        else if(nodesInBucket.size() < ksize && nodesInBucket.contains(node)){
            moveToLast(nodesInBucket, node);
        }

        //bucket full: ping least recent seen node in the bucket
        // if not reply, remove least recent seen node and add this node to tail
        // if reply, move this least recent seen node to tail. add new node as backup.
        else{
            Host lastNodeInBucket = nodesInBucket.get(-1);
            //may need RPC ping here
            Host pingNode = getLeastRecentSeen(nodesInBucket);
            //if: pingNode not reply back (comment now for test)
//            if(pingNode.replyRPC() == false) {
//                nodesInBucket.remove(pingNode);
//                nodesInBucket.add(node);
//            }
//            else{
//                moveToLast(nodesInBucket, pingNode);
//                nodesBackup.add(node);
                System.out.println("can not add this node, all nodes in the bucket are alive!");
//            }
        }
    }


    /**
     * update the network by refresh the k-buckets
     */
    public void BucketRefreshing(){
        //ping all nodes in the bucket to keep alive nodes only
        ArrayList<Host> copyNodesInBucket = this.nodesInBucket;
        for (int i = 1; i < copyNodesInBucket.size(); i++) {

            Host curInBucket = copyNodesInBucket.get(i);
            //RPC ping cur, if cur not reply, else keep cur (no action performed)
            nodesInBucket.remove(curInBucket);
        }
        //refill nodes(from backup) to the bucket
        while(nodesInBucket.size() < ksize && !nodesBackup.isEmpty()){
            Host curBackup = nodesBackup.get(0);
            //check this cur host is still alive
            //RPC ping cur
            //if cur is alive, add cur to bucket. Otherwise continue.
            nodesBackup.remove(curBackup);
            nodesInBucket.add(curBackup);
            }
        }

    }



