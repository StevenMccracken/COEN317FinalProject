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
        selfClient.put(0b010, new DataBlock(5));
        var r1 = selfClient.get(0b111);
        var r2 = selfClient.get(0b010);

        ASSERT(r1.sampleValue == 1);
        ASSERT(r2.sampleValue == 5);
    }

    public static void testJoin() {
        System.out.println("TEST NODE JOIN");
        DummyNetwork network = new DummyNetwork(2);
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

        ASSERT(client1.allHosts().contains(host1));
        ASSERT(client1.allHosts().contains(host2));
        ASSERT(client1.allHosts().contains(host3));
        ASSERT(client1.allHosts().contains(host4));
    }

    public static void testLeave() {
        System.out.println("TEST NODE LEAVE");
        DummyNetwork network = new DummyNetwork(4);
        Host host1 = new Host("ip0111", 0b0111, 8000);
        Host host2 = new Host("ip0011", 0b0011, 8000);
        Host host3 = new Host("ip1011", 0b1011, 8000);
        Host host4 = new Host("ip0110", 0b0110, 8000);
        Host host5 = new Host("ip1111", 0b1111, 8000);
        KademliaClient client1 = network.addHost(host1);
        client1.addHost(host2);
        client1.addHost(host3);
        client1.addHost(host4);
        client1.addHost(host5);

        client1.getClosestNode(0b0111);
        // unfinished
    }

    public static void main(String[] args) {
//        testRPC();
        testRouteTree();
        testJoin();
        testLeave();
    }
}