package edu.scu.kademlia;

import lombok.RequiredArgsConstructor;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;

// This is just a sample, it pretends to be several hosts
@RequiredArgsConstructor
class DummyRPC implements KademliaRPC {

    final Map<Host, KademliaClient> dummyHosts = new HashMap<>();
    final int ksize;

    // This method is just for the dummy class to set up its hosts
    public KademliaClient addHost(Host host) {

        KademliaClient client = new KademliaClient(3, host, this, ksize);
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
    public List<Host> findNode(Host host, long key) {
        System.out.println("[Network] `findNode` to " + host.ip + " key " + key);
        KademliaClient client = dummyHosts.get(host);
        return client.nodeLookup(key);
    }

    @Override
    public HostSearchResult findValue(Host host, long key) {
        System.out.println("[Network] `findValue` to " + host.ip + " key " + key);
        KademliaClient client = dummyHosts.get(host);

        if (client.hasData(key)) {
            //noinspection OptionalGetWithoutIsPresent
            return new HostSearchResult(client.get(key).get());
        }
        return new HostSearchResult(client.getClosestHosts(key, ksize));
    }

    @Override
    public void store(Host host, long key, DataBlock data) {
        System.out.println("[Network] `store` to " + host.ip + " key " + key);
        KademliaClient client = dummyHosts.get(host);
        client.put(key, data);
    }

    //dummy function for test purpose
    @Override
    public boolean ping(Host host) {
        return true;

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
    final static int KSIZE = 1;

    public static void ASSERT(boolean bool) {
        if (!bool) {
            throw new AssertionError("Condition was false!");
        }
    }

    public static void testRouteTree() {
        System.out.println("TEST ROUTE TREE");
        DummyRPC rpc = new DummyRPC(KSIZE);
        Host self = new Host("ip111", 0b111, 8000);
        KademliaClient selfClient = rpc.addHost(self);
        rpc.addHost(new Host("ip000", 0b000, 8000));
        rpc.addHost(new Host("ip010", 0b010, 8000));
        rpc.addHost(new Host("ip110", 0b110, 8000));

        selfClient.put(0b111, new DataBlock(1));
        selfClient.put(0b011, new DataBlock(5));
        var r1 = selfClient.get(0b111);
        var r2 = selfClient.get(0b011);

        ASSERT(r1.get().sampleValue == 1);
        ASSERT(r2.get().sampleValue == 5);
    }

    public static void testNewNodeJoining() {
        System.out.println("TEST NEW NODE JOINING");
        DummyRPC rpc = new DummyRPC(KSIZE);
        Host self = new Host("ip111", 0b111, 8000);
        KademliaClient selfClient = rpc.addHost(self);
        Host newHost1 = new Host("ip000", 0b000, 8000);
        Host newHost2 = new Host("ip001", 0b001, 8000);
        Host newHost3 = new Host("ip011", 0b011, 8000);

        //test for exist node joining
        rpc.findNode(self, newHost1.getKey());
        rpc.findNode(self, newHost1.getKey());

        //test for new node join when bucket not full
        rpc.findNode(self, newHost2.getKey());

        //test for new node join when bucket full
        rpc.findNode(self, newHost3.getKey());
    }

    public static void testBucketrefreshing() {
        System.out.println("TEST BUCKET REFRESHING");
        DummyRPC rpc = new DummyRPC(KSIZE);
        Host self = new Host("ip111", 0b111, 8000);
        KademliaClient selfClient = rpc.addHost(self);
        Host newHost1 = new Host("ip000", 0b000, 8000);
        Host newHost2 = new Host("ip001", 0b001, 8000);
        Host newHost3 = new Host("ip011", 0b011, 8000);
        Host newHost4 = new Host("ip011", 0b010, 8000);

        //nodes joining the network
        rpc.findNode(self, newHost1.getKey());
        rpc.findNode(self, newHost2.getKey());
        rpc.findNode(self, newHost3.getKey());
        rpc.findNode(self, newHost4.getKey());

        //test for bucket refreshing
        System.out.println("start refreshing");
        Set<Bucket> buckets = selfClient.getAllBuckets();
        for(Bucket b: buckets){
//            System.out.println("bucketID: "+ b.getBucketID());
            b.refreshBucket();
        }
    }
//
//
//    public static void periodicallyBucketReshing() {
//        System.out.println("TEST PERIODICALLY BUCKET REFRESHING");
//        //initial local host
//        Host self = new Host("ip111", 0b111, 8000);
//        DummyRPC rpc = new DummyRPC(KSIZE);
//
//        //start time counting
//        long start = System.currentTimeMillis();
//
//        //update buckets once an hour. This can dealing with node leaving
//        while (true) {
//            //calculate in sec
//            float timeElapse = (System.currentTimeMillis()- start) / 1000F;
//            //1 hour = 3600 sec, do bucket refresh.
//            //set to 1 for test only
//            if (timeElapse > 1) {
//                for (Bucket b : self.getAllBuckets()) {
//                    b.BucketRefreshing();
//                }
//                break;//for test only
//                //reset start
////                start = System.currentTimeMillis();
//            }
//        }
//    }

    private static void testRPC() {
        try {
            final InetAddress inetAddress = InetAddress.getLocalHost();
            final String hostAddress = inetAddress.getHostAddress();
            final String[] hostAddressParts = hostAddress.split("\\.");
            long encodedHostAddress = 0;
            for (int i = 0; i < 4; i++) {
                encodedHostAddress += Integer.parseInt(hostAddressParts[i]) << (24 - (8 * i));
            }

            final Host host = new Host(hostAddress, encodedHostAddress, 8000);
            final KademliaRPC rpc = new KademliaRPCImpl();
            final KademliaClient client = new KademliaClient(32, host, rpc, KSIZE);
            final Scanner input = new Scanner(System.in);

            int val = 0;
            while (val != 42) {
                final int a = input.nextInt();
                System.out.println(a);
                client.put(encodedHostAddress, new DataBlock(a));
            }

            final Optional<DataBlock> r1 = client.get(encodedHostAddress);
            final Optional<DataBlock> r2 = client.get(0b011); // How to get a real address here?

            System.out.println(r1.get().sampleValue);
            System.out.println(r2.get().sampleValue);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
//        testRouteTree();
//        testBucketrefreshing();
//        testNewNodeJoining();
//        periodicallyBucketReshing();
        testRPC();
    }
}