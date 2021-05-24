package edu.scu.kademlia;

import java.util.HashMap;
import java.util.Map;

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
}

public class Main {
    public static void testRouteTree() {
        DummyRPC rpc = new DummyRPC();
        Host self = new Host("ip111", 0b111, 8000);
        KademliaClient selfClient = rpc.addHost(self);
        rpc.addHost(new Host("ip000", 0b000, 8000));
        rpc.addHost(new Host("ip010", 0b010, 8000));
        rpc.addHost(new Host("ip110", 0b110, 8000));

        selfClient.put(0b111, new DataBlock(1));
        selfClient.put(0b011, new DataBlock(5));
        var r1 = selfClient.get(0b111);
        var r2 = selfClient.get(0b011);
    }

    public static void testBucketrefreshing() {
        Host self = new Host("ip111", 0b111, 8000);
        Bucket myBucket = new Bucket(5,3, self);
        myBucket.BucketRefreshing();
    }

    public static void main(String[] args) {
        testRouteTree();
//        testBucketrefreshing();
    }
}
