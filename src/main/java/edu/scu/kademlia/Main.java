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
            return new HostSearchResult(client.get(key));
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

//    public static void testRouteTree() {
//        System.out.println("TEST ROUTE TREE");
//        DummyRPC rpc = new DummyRPC(KSIZE);
//        Host self = new Host("ip111", 0b111, 8000);
//        KademliaClient selfClient = rpc.addHost(self);
//        rpc.addHost(new Host("ip000", 0b000, 8000));
//        rpc.addHost(new Host("ip010", 0b010, 8000));
//        rpc.addHost(new Host("ip110", 0b110, 8000));
//
//        selfClient.put(0b111, new DataBlock(1));
//        selfClient.put(0b011, new DataBlock(5));
//        var r1 = selfClient.get(0b111);
//        var r2 = selfClient.get(0b011);
//
//        ASSERT(r1.get().sampleValue == 1);
//        ASSERT(r2.get().sampleValue == 5);
//    }

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
    public static void periodicallyKeyValueRestoring() {
        System.out.println("TEST PERIODICALLY key-value RESTORING");
        try {
            final InetAddress inetAddress = InetAddress.getLocalHost();
//            final String hostAddress = inetAddress.getHostAddress();
            final String hostAddress = "192.158.1.38";
            final String[] hostAddressParts = hostAddress.split("\\.");
            long encodedHostAddress = 0;
            for (int i = 0; i < 4; i++) {
                encodedHostAddress |= Integer.parseInt(hostAddressParts[i]) << (48 - (8 * i));
            }

            int port1 = 8000;
            final long encodedHostAddress1 = encodedHostAddress | (port1 << 16);
            String key1 = Long.toBinaryString(encodedHostAddress1);
            System.out.println(key1);

            int port2 = 8001;
            final long encodedHostAddress2 = encodedHostAddress | (port2 << 16);
            String key2 = Long.toBinaryString(encodedHostAddress2);
            System.out.println(key2);

            Host self = new Host(hostAddress, encodedHostAddress1, port1);
            Host host1 = new Host(hostAddress, encodedHostAddress2, port2);

            KademliaRPC selfrpc = new KademliaRPCImpl();
            KademliaRPC rpc1 = new KademliaRPCImpl();
            int bitLen = 32;
            KademliaClient selfClient = new KademliaClient(bitLen, self, selfrpc, KSIZE);
            KademliaClient Client1 = new KademliaClient(bitLen, host1, rpc1, KSIZE);
            selfClient.addHost(host1);

            selfClient.put(3030, new DataBlock(3030));

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
                        selfrpc.store(h, key, selfClient.getDataStore().get(key));
                    }
                }
//                break;
                start = System.currentTimeMillis();
            }
        }

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public static void periodicallyBucketRefreshing() {
        System.out.println("TEST PERIODICALLY BUCKET REFRESHING");
        try {
            final InetAddress inetAddress = InetAddress.getLocalHost();
//            final String hostAddress = inetAddress.getHostAddress();
            final String hostAddress = "192.158.1.38";
            final String[] hostAddressParts = hostAddress.split("\\.");
            long encodedHostAddress = 0;
            for (int i = 0; i < 4; i++) {
                encodedHostAddress |= Integer.parseInt(hostAddressParts[i]) << (48 - (8 * i));
            }

            int port1 = 8000;
            final long encodedHostAddress1 = encodedHostAddress | (port1 << 16);
            String key1 = Long.toBinaryString(encodedHostAddress1);
            System.out.println(key1);

            int port2 = 8001;
            final long encodedHostAddress2 = encodedHostAddress | (port2 << 16);
            String key2 = Long.toBinaryString(encodedHostAddress2);
            System.out.println(key2);

            int port3 = 8002;
            final long encodedHostAddress3 = encodedHostAddress | (port3 << 16);
            String key3 = Long.toBinaryString(encodedHostAddress3);
            System.out.println(key3);

            int port4 = 8003;
            final long encodedHostAddress4 = encodedHostAddress | (port4 << 16);
            String key4 = Long.toBinaryString(encodedHostAddress4);
            System.out.println(key4);

            Host self = new Host(hostAddress, encodedHostAddress1, port1);
            Host host1 = new Host(hostAddress, encodedHostAddress2, port2);
            Host host2 = new Host(hostAddress, encodedHostAddress3, port3);
            Host host3 = new Host(hostAddress, encodedHostAddress4, port4);

            KademliaRPC selfrpc = new KademliaRPCImpl();
            int bitLen = 32;
            KademliaClient selfClient = new KademliaClient(bitLen, self, selfrpc, KSIZE);
            selfClient.addHost(host1);
            selfClient.addHost(host2);
            selfClient.addHost(host3);

        //start time counting
        long start = System.currentTimeMillis();

        while (true) {
            //calculate in sec
            float timeElapse = (System.currentTimeMillis() - start) / 1000F;
            //1 hour = 3600 sec, do bucket refreshing.
            if (timeElapse > 3600) {
                //remove left nodes
                System.out.println("start bucket refreshing");
                for (Bucket b : selfClient.getAllBuckets()) {
                    b.refreshBucket();
                }
                //populate newly joined hosts into buckets
                for (int i = 0; i < bitLen; i++) {
                    long lowerBound = (long) Math.pow(2, i);
                    long upperBound = (long) Math.pow(2, i + 1);
                    long randomKey = lowerBound + (long) (Math.random() * (upperBound - lowerBound));
                    selfrpc.findNode(self, randomKey);
                }
                start = System.currentTimeMillis();
            }
        }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }


    private static void testRPC() {
        try {
            final InetAddress inetAddress = InetAddress.getLocalHost();
            final String hostAddress = inetAddress.getHostAddress();
            System.out.println("Your IP address is: " + hostAddress);
            final String[] hostAddressParts = hostAddress.split("\\.");

            final Random random = new Random();
            final int port = random.nextInt(65535) + 3000;
            System.out.println("Your assigned port is: " + port);
            long encodedHostAddress = 0;
            for (int i = 0; i < 4; i++) {
                encodedHostAddress |= Integer.parseInt(hostAddressParts[i]) << (48 - (8 * i));
            }
            encodedHostAddress |= port << 16;
            System.out.println("Your local key is: " + encodedHostAddress);

            final Host host = new Host(hostAddress, encodedHostAddress, port);
            final KademliaRPC rpc = new KademliaRPCImpl();
            final KademliaClient client = new KademliaClient(32, host, rpc, KSIZE);
            final Scanner input = new Scanner(System.in);

            while (true) {
                System.out.println("Enter remote host IP: ");
                final String remoteIP = input.nextLine();
                final String[] remoteIPParts = hostAddress.split("\\.");
                System.out.println("Enter remote host port: ");
                final int remotePort = input.nextInt();
                long remoteAddress = 0;
                for (int i = 0; i < 4; i++) {
                    remoteAddress |= Integer.parseInt(remoteIPParts[i]) << (48 - (8 * i));
                }
                remoteAddress |= remotePort << 16;
                System.out.println("The remote host's key is: " + remoteAddress);
                final Host remoteHost = new Host(remoteIP, remoteAddress, remotePort);
                client.addHost(remoteHost);
                rpc.ping(remoteHost);

                System.out.println("Enter a value to write to remote host: ");
                final int value = input.nextInt();
                client.put(remoteAddress, new DataBlock(value));

                System.out.println("Displaying any data written to this host in the meantime");
                System.out.println(client.getDataStore());
                System.out.println(client.allHosts());
                System.out.flush();
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private static void testNodeJoining() {
        try {
            final InetAddress inetAddress = InetAddress.getLocalHost();
//            final String hostAddress = inetAddress.getHostAddress();
            final String hostAddress = "192.158.1.38";
            final String[] hostAddressParts = hostAddress.split("\\.");
            long encodedHostAddress = 0;
            for (int i = 0; i < 4; i++) {
                encodedHostAddress |= Integer.parseInt(hostAddressParts[i]) << (48 - (8 * i));
            }

            int port1 = 8000;
            final long encodedHostAddress1 = encodedHostAddress | (port1 << 16);
            String key1 = Long.toBinaryString(encodedHostAddress1);
            System.out.println(key1);

            int port2 = 8001;
            final long encodedHostAddress2 = encodedHostAddress | (port2 << 16);
            String key2 = Long.toBinaryString(encodedHostAddress2);
            System.out.println(key2);

            int port3 = 8002;
            final long encodedHostAddress3 = encodedHostAddress | (port3 << 16);
            String key3 = Long.toBinaryString(encodedHostAddress3);
            System.out.println(key3);

            Host self = new Host(hostAddress, encodedHostAddress1, port1);
            Host newhost1 = new Host(hostAddress, encodedHostAddress2, port2);
            Host newhost2 = new Host(hostAddress, encodedHostAddress3, port3);

            KademliaRPC selfrpc = new KademliaRPCImpl();
            KademliaRPC rpc1 = new KademliaRPCImpl();
            KademliaRPC rpc2 = new KademliaRPCImpl();

            KademliaClient selfClient = new KademliaClient(32, self, selfrpc, KSIZE);
            KademliaClient newClient1 = new KademliaClient(32, newhost1, rpc1, KSIZE);
            KademliaClient newClient2 = new KademliaClient(32, newhost2, rpc2, KSIZE);

            selfClient.addHost(newhost1);
            newClient1.addHost(self);
            System.out.println("self: "+ self + " "+selfClient.allHosts());
            System.out.println("new1: "+ newClient1.allHosts());
            System.out.println("new2: "+ newClient2.allHosts());

            // NN(new node) sends a LookupRequest(A.NodeId) to BN(bootstrap node, self). A Lookup Request basically asks the receiving node for the K-Closest nodes it knows to a given NodeId.
            // In this case, BN will return the K-Closest nodes it knows to NN.
            List<Host> closerNodes = rpc2.findNode(self, newhost2.getKey());
            // BN will now add NN to it's routing table, so NN is now in the network.
            selfClient.addHost(newhost2);
            // NN receives the list of K-Closest nodes to itself from BN. NN adds BN to it's routing table.
            newClient2.addHost(self);
            // NN now pings these K nodes received from BN, and the ones that reply are added to it's Routing Table in the necessary buckets based on distance. By pinging these nodes, they also learn of NN existence and add NN to their Routing tables.
            for(Host h: closerNodes){
                if(rpc2.ping(h)){
                    newClient2.addHost(h);
                }
            }
            // NN is now connected to the network and is known by nodes on the network.

            System.out.println("self: "+ selfClient.allHosts());
            System.out.println("new1: "+newClient1.allHosts());
            System.out.println("new2: "+newClient2.allHosts());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
    testRPC();
    }
}