package edu.scu.kademlia;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Host {
    String ip;
    int key;

    // udp port
    int port;

    //time stamp for each node, RPC can update this value
    private long mostRecentSeen = System.currentTimeMillis();
    int bitLen;

    // initial kbuckets and k value
    Bucket[] buckets;
    int ksize;


    public Host(String ip, int key, int port) {
        this.ip = ip;
        this.key = key;
        this.port = port;

        //initial bucket for each range
        for (int i = 0; i < bitLen; i++) {
            this.buckets[i] = new Bucket(ksize, i, this);
        }
    }

    //TODO: adding time stamp function to buckets
    public Host(){
        long start = System.currentTimeMillis();
        //update buckets once an hour. This can dealing with node leaving
        while(true){
            //calculate in sec
            float timeElapse = (start  - System.currentTimeMillis())/1000F;
            //1 hour = 3600 sec, do bucket refresh
            if(timeElapse > 1){
                for(Bucket b: this.buckets) {
                    b.BucketRefreshing();
                }
            }
        }
    }


    public long getLastSeenTime(){
        return this.mostRecentSeen;
    }

    /**
     * Find the closet bucket to put new node info
     * Dealing with new node joining the network
     * @param host known node in the network and the joining node
     */
    private int findClosetBucket(Host knownHost, Host newHost){
        int position = 0;
        String strKnown, strNew;
        strKnown = Integer.toBinaryString(knownHost.key);
        strNew = Integer.toBinaryString(newHost.key);
        while(position < strKnown.length()){
            if(strKnown.charAt(position) != strNew.charAt(position)){
                break;
            }
            position++;
        }

        return position - 1;
    }

    /**
     * For new nodes joining the network. The joining node must know a node in the network
     * @param host known node in the network and the joining node
     */
    public void NodeJoin(Host knownHost, Host newHost) {
        int position = findClosetBucket(knownHost, newHost);
        knownHost.buckets[position].addNodeToBucket(newHost);
    }

}
