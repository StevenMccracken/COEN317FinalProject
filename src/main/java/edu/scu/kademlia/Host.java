package edu.scu.kademlia;

import lombok.AllArgsConstructor;

import java.util.ArrayList;

@AllArgsConstructor
public class Host {
    String ip;
    int key;

    // udp port
    int port;

    //time stamp for each node, RPC can update this value
    private long mostRecentSeen;
    private int bitLen;


    // initial kbuckets and k value
    ArrayList<Bucket> buckets = new ArrayList<Bucket>();
    int ksize;


    public Host(String ip, int key, int port, int ksize) {
        this.ip = ip;
        this.key = key;
        this.port = port;

        this.mostRecentSeen = System.currentTimeMillis();
        this.bitLen = Integer.toBinaryString(key).length();
        this.ksize = ksize;

        //initial bucket for each range
        for (int i = 0; i < bitLen; i++) {
            buckets.add(new Bucket(ksize, i, this));
        }
    }



    public long getLastSeenTime(){
        return this.mostRecentSeen;
    }

    public ArrayList<Bucket> getBuckets(){
        return buckets;
    }

//    /**
//     * Find the closet bucket to put new node info
//     * Dealing with new node joining the network
//     * @param host known node in the network and the joining node
//     */
//    public int findClosetBucket(Host knownHost, Host newHost){
//        int position = 0;
//        String strKnown, strNew;
//        strKnown = Integer.toBinaryString(knownHost.key);
//        strNew = Integer.toBinaryString(newHost.key);
//        while(position < strKnown.length()){
//            if(strKnown.charAt(position) != strNew.charAt(position)){
//                break;
//            }
//            position++;
//        }
//        return position - 1;
//    }

//    /**
//     * For new nodes joining the network. The joining node must know a node in the network
//     * @param host known node in the network and the joining node
//     */
//    public void NodeJoin(Host knownHost, Host newHost) {
//        int position = findClosetBucket(knownHost, newHost);
//        knownHost.buckets.get(position).addNodeToBucket(newHost);
//    }

}
