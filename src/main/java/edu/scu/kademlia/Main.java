package edu.scu.kademlia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

// This is just a sample, it pretends to be several hosts
class DummyRPC implements KademliaRPC {

    Map<Host, KademliaClient> dummyHosts = new HashMap<>();

    // This method is just for the dummy class to set up its hosts
    public KademliaClient addHost(Host host) {
        KademliaClient client = new KademliaClient(3, host, this);
        dummyHosts.put(host, client);

        // all trees have all nodes if needed
        for (Host h : dummyHosts.keySet()) {
            client.addHost(h);
        }
        for (KademliaClient t : dummyHosts.values()) {
            t.addHost(host);
        }

        return client;
    }

    @Override
    public Host findHost(Host host, int key) {
        KademliaClient client = dummyHosts.get(host);
        return client.getClosestHost(key);
    }

    @Override
    public HostSearchResult findValue(Host host, int key) {
        System.out.println("[Network] `findValue` to " + host.ip + " key " + key);
        KademliaClient client = dummyHosts.get(host);

        if (client.hasData(key)) {
            //noinspection OptionalGetWithoutIsPresent
            return new HostSearchResult(client.get(key).get());
        }
        return new HostSearchResult(client.getClosestHost(key));
    }

    @Override
    public void store(Host host, int key, DataBlock data) {
        System.out.println("[Network] `store` to " + host.ip + " key " + key);
        KademliaClient client = dummyHosts.get(host);
        client.put(key, data);
    }

    @Override
    public boolean ping(Host host) {
        return dummyHosts.containsKey(host);
    }

    //dummy function for node join
    @Override
    public void findNode(KademliaClient client, Host host) {
        //
        if(dummyHosts.containsKey(host)){
            System.out.println("[dummy Network findNode] host already exist in the network");
            return;
        }

        client.NodeJoin(host, this);
    }

    //dummy function for test purpose
    @Override
    public boolean pingNode(Host host) {
        Random rand = new Random();
        int int_random = rand.nextInt(2);
        System.out.println("[dummy Network pingNode] random int: " + int_random);
        if(int_random == 0){
            return false;
        }
        return true;
    }

}

public class Main {
    public static void testRouteTree() {
        DummyRPC rpc = new DummyRPC();
        Host self = new Host("ip111", 0b111, 8000, 2);
        KademliaClient selfClient = rpc.addHost(self);
        rpc.addHost(new Host("ip000", 0b000, 8000, 2));
        rpc.addHost(new Host("ip010", 0b010, 8000, 2));
        rpc.addHost(new Host("ip110", 0b110, 8000, 2));

        selfClient.put(0b111, new DataBlock(1));
        selfClient.put(0b011, new DataBlock(5));
        var r1 = selfClient.get(0b111);
        var r2 = selfClient.get(0b011);
    }

    public static void testNewNodeJoining() {
        DummyRPC rpc = new DummyRPC();
        Host self = new Host("ip111", 0b111, 8000, 2);
        KademliaClient selfClient = rpc.addHost(self);
        Host newHost1 = new Host("ip000", 0b000, 8000, 2);
        Host newHost2 = new Host("ip001", 0b001, 8000, 2);
        Host newHost3 = new Host("ip011", 0b011, 8000, 2);

        //test for exist node joining
        rpc.findNode(selfClient, newHost1);
        rpc.findNode(selfClient, newHost1);

        //test for new node join when bucket not full
        rpc.findNode(selfClient, newHost2);

        //test for new node join when bucket full
        rpc.findNode(selfClient, newHost3);

    }
    public static void testBucketrefreshing() {
        DummyRPC rpc = new DummyRPC();
        Host self = new Host("ip111", 0b111, 8000, 2);
        KademliaClient selfClient = rpc.addHost(self);
        Host newHost1 = new Host("ip000", 0b000, 8000, 2);
        Host newHost2 = new Host("ip001", 0b001, 8000, 2);
        Host newHost3 = new Host("ip011", 0b011, 8000, 2);
        Host newHost4 = new Host("ip011", 0b010, 8000, 2);

        //nodes joining the network
        rpc.findNode(selfClient, newHost1);
        rpc.findNode(selfClient, newHost2);
        rpc.findNode(selfClient, newHost3);
        rpc.findNode(selfClient, newHost4);

        //test for bucket refreshing
        System.out.println("start refreshing");
        ArrayList<Bucket> buckets = self.getBuckets();
        for(Bucket b: buckets){
            System.out.println("bucketID: "+ b.getBucketID());
            b.BucketRefreshing(rpc);
        }
    }


    public static void periodicallyBucketReshing() {
        //initial local host
        Host self = new Host("ip111", 0b111, 8000, 2);
        DummyRPC rpc = new DummyRPC();

        //start time counting
        long start = System.currentTimeMillis();

        //update buckets once an hour. This can dealing with node leaving
        while (true) {
            //calculate in sec
            float timeElapse = (System.currentTimeMillis()- start) / 1000F;
            //1 hour = 3600 sec, do bucket refresh.
            //set to 1 for test only
            if (timeElapse > 1) {
                for (Bucket b : self.getBuckets()) {
                    b.BucketRefreshing(rpc);
                }
                break;//for test only
                //reset start
//                start = System.currentTimeMillis();
            }
        }
    }




    public static void main(String[] args) {
        testRouteTree();
        testBucketrefreshing();
        testNewNodeJoining();
        periodicallyBucketReshing();

    }
}