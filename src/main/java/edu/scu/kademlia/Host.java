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
            buckets.add(new Bucket(ksize));
        }
    }

    public long getLastSeenTime(){
        return this.mostRecentSeen;
    }

    public ArrayList<Bucket> getBuckets(){
        return buckets;
    }

}
