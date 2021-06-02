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
    public static void periodicallyKeyValueRestoring() {
        System.out.println("TEST PERIODICALLY key-value RESTORING");
        //initial local host
        Host self = new Host("ip111", 0b111, 8000);
        DummyRPC rpc = new DummyRPC(KSIZE);
        KademliaClient selfClient = new KademliaClient(3, self, rpc, KSIZE);

        //start time counting
        long start = System.currentTimeMillis();

        while (true) {
            //calculate in sec
            float timeElapse = (System.currentTimeMillis() - start) / 1000F;
            //24 hour = 86400 sec, do key-pair republishing.
            if (timeElapse > 86400) {
                for (long key : selfClient.getDataStore().keySet()) {
                    List<Host> hosts = selfClient.nodeLookup(key);
                    for(Host h: hosts){
                        rpc.store(h, key, selfClient.getDataStore().get(key));
                    }
                }
                start = System.currentTimeMillis();
            }
        }
    }

    public static void periodicallyBucketRefreshing() {
        System.out.println("TEST PERIODICALLY BUCKET REFRESHING");
        //initial local host
        Host self = new Host("ip111", 0b111, 8000);
        DummyRPC rpc = new DummyRPC(KSIZE);
        int bitLen = 3;
        KademliaClient selfClient = new KademliaClient(bitLen, self, rpc, KSIZE);

        //start time counting
        long start = System.currentTimeMillis();

        while (true) {
            //calculate in sec
            float timeElapse = (System.currentTimeMillis() - start) / 1000F;
            //1 hour = 3600 sec, do bucket refreshing.
            if (timeElapse > 1) {
                //remove left nodes
                for(Bucket b: selfClient.getAllBuckets()){
                    b.refreshBucket();
                }
                //populate newly joined hosts into buckets
                for(int i=0; i< bitLen; i++){
                    long lowerBound = (long) Math.pow(2, i);
                    long upperBound = (long) Math.pow(2, i+1);
                    long randomKey = lowerBound + (long) (Math.random() * (upperBound - lowerBound));
//                    System.out.println(randomKey);
                    rpc.findNode(self, randomKey);
                }
                start = System.currentTimeMillis();
            }
        }
    }


    private static void testRPC() {
        try {
            final InetAddress inetAddress = InetAddress.getLocalHost();
            final String hostAddress = inetAddress.getHostAddress();
            final String[] hostAddressParts = hostAddress.split("\\.");
            long encodedHostAddress = 0;
            for (int i = 0; i < 4; i++) {
                encodedHostAddress += Integer.parseInt(hostAddressParts[i]) << (24 - (8 * i));
            }

            System.out.println(hostAddress+" " + encodedHostAddress);

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

    private static void testNodeJoining() {
        try {
            final InetAddress inetAddress = InetAddress.getLocalHost();
            final String hostAddress = inetAddress.getHostAddress();
            final String[] hostAddressParts = hostAddress.split("\\.");
            long encodedHostAddress = 0;
            for (int i = 0; i < 4; i++) {
                encodedHostAddress += Integer.parseInt(hostAddressParts[i]) << (24 - (8 * i));
            }

//            final Host host = new Host(hostAddress, encodedHostAddress, 8000);
            int port1 = 8000;
            String key1 = Long.toBinaryString(encodedHostAddress+(long)port1);
            System.out.println(key1);
            int port2 = 8001;
            String key2 = Long.toBinaryString(encodedHostAddress+(long)port2);
            System.out.println(key2);

            int port3 = 8002;
            String key3 = Long.toBinaryString(encodedHostAddress+(long)port3);
            System.out.println(key3);

            Host self = new Host(hostAddress, encodedHostAddress+port1, port1);
            Host newhost1 = new Host(hostAddress, encodedHostAddress+port2, port2);
            Host newhost2 = new Host(hostAddress, encodedHostAddress+port3, port3);

            KademliaRPC selfrpc = new KademliaRPCImpl();
            KademliaRPC rpc1 = new KademliaRPCImpl();
            KademliaRPC rpc2 = new KademliaRPCImpl();

            KademliaClient selfClient = new KademliaClient(32, self, selfrpc, KSIZE);
            KademliaClient newClient1 = new KademliaClient(32, newhost1, rpc1, KSIZE);
            KademliaClient newClient2 = new KademliaClient(32, newhost2, rpc2, KSIZE);

            //newhost 1 knows self client and it is added to the network
            selfClient.addHost(newhost1);
            System.out.println("self: "+ selfClient.allHosts()); //if 2 in it should be correct
            //new client1 start self-lookup
            rpc1.findNode(newhost1, newhost1.getKey());
            System.out.println("new1: "+ newClient1.allHosts()); //if 2 in it should be correct

//            rpc2.findNode(newhost2.getKey());
//            System.out.println("new2: "+newClient2.allHosts());
//
//            System.out.println("self: "+selfClient.allHosts());


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
//        testRouteTree();
//        testBucketrefreshing();
//        testNewNodeJoining();

//        periodicallyBucketReshing();
//        periodicallyKeyValueRestoring();
//        periodicallyBucketRefreshing();
        testNodeJoining();
//        testRPC();
    }
}