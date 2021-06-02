package edu.scu.kademlia;

import lombok.RequiredArgsConstructor;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;

@RequiredArgsConstructor
class DummyNetwork {
    final Map<Host, KademliaClient> dummyHosts = new HashMap<>();
    final int ksize;

    // This method is just for the dummy class to set up its hosts
    public KademliaClient addHost(Host host) {
        DummyRPC rpc = new DummyRPC(this, host);
        KademliaClient client = new KademliaClient(4, host, rpc, ksize);
        Host introducer = null;
        if (!dummyHosts.isEmpty()) {
            introducer = dummyHosts.keySet().stream().findFirst().get();
        }
        dummyHosts.put(host, client);

        client.start(introducer);
        return client;
    }

    public void removeHost(Host host) {
        dummyHosts.remove(host);
    }

    public List<Host> findNode(Host src, Host dest, long key) {
        System.out.println("[Network] `findNode` to " + dest.ip + " key " + key);
        KademliaClient client = dummyHosts.get(dest);
        List<Host> result = client.findNode(key);
        client.addHost(src);
        return result;
    }

    public HostSearchResult findValue(Host src, Host dest, long key) {
        System.out.println("[Network] `findValue` to " + dest.ip + " key " + key);
        KademliaClient client = dummyHosts.get(dest);
        HostSearchResult result = client.findValue(key);
        client.addHost(src);
        return result;
    }

    public void store(Host src, Host dest, long key, DataBlock data) {
        System.out.println("[Network] `store` to " + dest.ip + " key " + key);
        KademliaClient client = dummyHosts.get(dest);
        client.store(key, data);
    }

    public boolean ping(Host host) {
        return dummyHosts.containsKey(host);
    }
}

// This is just a sample, it pretends to be several hosts
@RequiredArgsConstructor
class DummyRPC implements KademliaRPC {

    final DummyNetwork network;
    final Host self;

    @Override
    public List<Host> findNode(Host host, long key) {
        return network.findNode(self, host, key);
    }

    @Override
    public HostSearchResult findValue(Host host, long key) {
        return network.findValue(self, host, key);
    }

    @Override
    public void store(Host host, long key, DataBlock data) {
        network.store(self, host, key, data);
    }

    @Override
    public boolean ping(Host host) {
        return network.ping(host);

//        Random rand = new Random();
//        int int_random = rand.nextInt(2);
//        System.out.println("[dummy Network pingNode] random int: " + int_random);
//        if(int_random == 0){
//            return false;
//        }
//        return true;
    }

}

public class Main {
    public static void ASSERT(boolean bool) {
        if (!bool) {
            throw new AssertionError("Condition was false!");
        }
    }

    public static void testRouteTree() {
        System.out.println("TEST ROUTE TREE");
        DummyNetwork network = new DummyNetwork(1);
        Host self = new Host("ip111", 0b111, 8000);
        KademliaClient selfClient = network.addHost(self);
        KademliaClient selfClient1 = network.addHost(new Host("ip000", 0b000, 8000));
        KademliaClient selfClient2 = network.addHost(new Host("ip010", 0b010, 8000));
        KademliaClient selfClient3 = network.addHost(new Host("ip110", 0b110, 8000));

        selfClient.put(0b111, new DataBlock(1));
        selfClient.put(0b011, new DataBlock(5));
        var r1 = selfClient.get(0b111);
        var r2 = selfClient.get(0b011);

        ASSERT(r1.sampleValue == 1);
        ASSERT(r2.sampleValue == 5);
    }

    public static void testJoin() {
        System.out.println("TEST NODE JOIN");
        DummyNetwork network = new DummyNetwork(2);
        Host self = new Host("ip0000", 0b0000, 8000);
        KademliaClient selfClient = network.addHost(self);
        KademliaClient selfClient1 = network.addHost(new Host("ip0001", 0b0001, 8000));
        KademliaClient selfClient2 = network.addHost(new Host("ip1000", 0b1000, 8000));


        selfClient.put(0b111, new DataBlock(1));
        selfClient.put(0b011, new DataBlock(5));
        var r1 = selfClient.get(0b111);
        var r2 = selfClient.get(0b011);

        ASSERT(r1.sampleValue == 1);
        ASSERT(r2.sampleValue == 5);
    }

//    public static void testNewNodeJoining() {
//        System.out.println("TEST NEW NODE JOINING");
//        DummyRPC rpc = new DummyRPC(KSIZE);
//        Host self = new Host("ip111", 0b111, 8000);
//        KademliaClient selfClient = rpc.addHost(self);
//        Host newHost1 = new Host("ip000", 0b000, 8000);
//        Host newHost2 = new Host("ip001", 0b001, 8000);
//        Host newHost3 = new Host("ip011", 0b011, 8000);
//
//        //test for exist node joining
//        rpc.findNode(self, newHost1.getKey());
//        rpc.findNode(self, newHost1.getKey());
//
//        //test for new node join when bucket not full
//        rpc.findNode(self, newHost2.getKey());
//
//        //test for new node join when bucket full
//        rpc.findNode(self, newHost3.getKey());
//    }

    //    public static void testBucketrefreshing() {
//        System.out.println("TEST BUCKET REFRESHING");
//        DummyRPC rpc = new DummyRPC(KSIZE);
//        Host self = new Host("ip111", 0b111, 8000);
//        KademliaClient selfClient = rpc.addHost(self);
//        Host newHost1 = new Host("ip000", 0b000, 8000);
//        Host newHost2 = new Host("ip001", 0b001, 8000);
//        Host newHost3 = new Host("ip011", 0b011, 8000);
//        Host newHost4 = new Host("ip011", 0b010, 8000);
//
//        //nodes joining the network
//        rpc.findNode(self, newHost1.getKey());
//        rpc.findNode(self, newHost2.getKey());
//        rpc.findNode(self, newHost3.getKey());
//        rpc.findNode(self, newHost4.getKey());
//
//        //test for bucket refreshing
//        System.out.println("start refreshing");
//        Set<Bucket> buckets = selfClient.getAllBuckets();
//        for(Bucket b: buckets){
////            System.out.println("bucketID: "+ b.getBucketID());
//            b.refreshBucket();
//        }
//    }
//
//
    public static void main(String[] args) {
//        testRPC();
        testRouteTree();
    }
}