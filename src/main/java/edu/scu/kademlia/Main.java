package edu.scu.kademlia;

import lombok.RequiredArgsConstructor;

import java.rmi.ConnectException;
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

    public List<Host> findNode(Host src, Host dest, long key, boolean isNew) {
        System.out.println("[Network] `findNode` " + src.ip + " to " + dest.ip + " key " + key);
        KademliaClient client = dummyHosts.get(dest);
        List<Host> result = client.findNode(key);
        client.addHost(src);
        if (isNew) {
            client.replicateClosest(src);
        }
        return result;
    }

    public HostSearchResult findValue(Host src, Host dest, long key) {
        System.out.println("[Network] `findValue` " + src.ip + " to " + dest.ip + " key " + key);
        KademliaClient client = dummyHosts.get(dest);
        HostSearchResult result = client.findValue(key);
        client.addHost(src);
        return result;
    }

    public void store(Host src, Host dest, long key, DataBlock data) {
        System.out.println("[Network] `store` " + src.ip + " to " + dest.ip + " key " + key);
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
    public List<Host> findNode(Host host, long key, boolean isNew) throws ConnectException {
        if (!network.ping(host)) {
            throw new ConnectException("Host offline");
        }
        return network.findNode(self, host, key, isNew);
    }

    @Override
    public HostSearchResult findValue(Host host, long key) throws ConnectException {
        if (!network.ping(host)) {
            throw new ConnectException("Host offline");
        }
        return network.findValue(self, host, key);
    }

    @Override
    public void store(Host host, long key, DataBlock data) throws ConnectException {
        if (!network.ping(host)) {
            throw new ConnectException("Host offline");
        }
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

    public static void testDataStore() {
        System.out.println("TEST DATA STORE");
        DummyNetwork network = new DummyNetwork(1);
        Host host1 = new Host("ip111", 0b0111, 8000);
        Host host2 = new Host("ip000", 0b0000, 8000);
        Host host3 = new Host("ip010", 0b0010, 8000);
        Host host4 = new Host("ip110", 0b0110, 8000);
        KademliaClient client1 = network.addHost(host1);
        KademliaClient client2 = network.addHost(host2);
        KademliaClient client3 = network.addHost(host3);
        KademliaClient client4 = network.addHost(host4);

        System.out.println("NETWORK READY");

        client1.put(0b111, new DataBlock(1));
        client1.put(0b010, new DataBlock(5));

        client1.printDataStore();
        client2.printDataStore();
        client3.printDataStore();
        client4.printDataStore();
    }

    public static void testJoin() {
        System.out.println("TEST NODE JOIN");
        DummyNetwork network = new DummyNetwork(1);
        Host host1 = new Host("ip111", 0b0111, 8000);
        Host host2 = new Host("ip000", 0b0000, 8000);
        Host host3 = new Host("ip010", 0b0010, 8000);
        Host host4 = new Host("ip110", 0b0110, 8000);
        System.out.println("ADD " + host1.toString());
        KademliaClient client1 = network.addHost(host1);
        System.out.println("ADD " + host2.toString());
        KademliaClient client2 = network.addHost(host2);
        System.out.println("ADD " + host3.toString());
        KademliaClient client3 = network.addHost(host3);
        System.out.println("ADD " + host4.toString());
        KademliaClient client4 = network.addHost(host4);
        client1.printHosts();
        client2.printHosts();
        client3.printHosts();
        client4.printHosts();
    }

    public static void testLeave() {
        System.out.println("TEST NODE LEAVE");
        DummyNetwork network = new DummyNetwork(3);
        Host host1 = new Host("ip0000", 0b0000, 8000);
        Host host2 = new Host("ip0001", 0b0001, 8000);
        Host host3 = new Host("ip1000", 0b1000, 8000);
        Host host4 = new Host("ip1100", 0b1100, 8000);
        Host host5 = new Host("ip1010", 0b1010, 8000);
        KademliaClient client1 = network.addHost(host1);
        KademliaClient client2 = network.addHost(host2);
        KademliaClient client3 = network.addHost(host3);
        KademliaClient client4 = network.addHost(host4);
        KademliaClient client5 = network.addHost(host5);

        System.out.println("SETUP");

        client1.put(0b1100, new DataBlock(10));
        client1.put(0b0001, new DataBlock(15));
        client1.printDataStore();
        client2.printDataStore();
        client3.printDataStore();
        client4.printDataStore();
        client5.printDataStore();
        System.out.println("Get 0b1100: " + client1.get(0b1100));
        network.removeHost(host4);
        System.out.println("Get 0b1100: " + client1.get(0b1100));

    }

    public static void testRepublish() {
        System.out.println("TEST NODE LEAVE");
        DummyNetwork network = new DummyNetwork(3);
        Host host1 = new Host("ip0000", 0b0000, 8000);
        Host host2 = new Host("ip0001", 0b0001, 8000);
        Host host3 = new Host("ip1000", 0b1000, 8000);
        Host host4 = new Host("ip1100", 0b1100, 8000);
        Host host5 = new Host("ip1010", 0b1010, 8000);
        KademliaClient client1 = network.addHost(host1);
        KademliaClient client2 = network.addHost(host2);
        KademliaClient client3 = network.addHost(host3);
        KademliaClient client4 = network.addHost(host4);
        KademliaClient client5 = network.addHost(host5);

        System.out.println("SETUP");

        long testKey = 0b1010;
        client5.store(testKey, new DataBlock(25));
        client1.printDataStore();
        client2.printDataStore();
        client3.printDataStore();
        client4.printDataStore();
        client5.printDataStore();
        client5.republish();
        client1.printDataStore();
        client2.printDataStore();
        client3.printDataStore();
        client4.printDataStore();
        client5.printDataStore();
        client5.republish();
        client1.printDataStore();
        client2.printDataStore();
        client3.printDataStore();
        client4.printDataStore();
        client5.printDataStore();

    }

    public static void testJoinReplication() {
        System.out.println("TEST NODE JOIN REPLICATION");
        DummyNetwork network = new DummyNetwork(3);
        Host host1 = new Host("ip0000", 0b0000, 8000);
        Host host2 = new Host("ip0001", 0b0001, 8000);
        Host host3 = new Host("ip1000", 0b1000, 8000);
        Host host4 = new Host("ip1100", 0b1100, 8000);
        Host host5 = new Host("ip1010", 0b1010, 8000);
        KademliaClient client1 = network.addHost(host1);
        KademliaClient client2 = network.addHost(host2);
        KademliaClient client3 = network.addHost(host3);

        System.out.println("SETUP");
        client3.store(0b1010, new DataBlock(30));
        client3.store(0b1100, new DataBlock(32));
        client1.printDataStore();
        client2.printDataStore();
        client3.printDataStore();

        KademliaClient client4 = network.addHost(host4);
        KademliaClient client5 = network.addHost(host5);
        client1.printDataStore();
        client2.printDataStore();
        client3.printDataStore();
        client4.printDataStore();
        client5.printDataStore();

    }


    public static void main(String[] args) {
//        testRPC();
//        testDataStore();
//        testJoin();
//        testLeave();
//        testRepublish();
        testJoinReplication();
    }
}