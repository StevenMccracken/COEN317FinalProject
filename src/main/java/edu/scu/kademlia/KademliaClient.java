package edu.scu.kademlia;

import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

public class KademliaClient {

    // The bits of the key to use
    private int bitLen;

    // The root of our route tree. Each node contains a kbucket.
    private RouteNode kbucketTree;

    // The hosts in our address book. Points to hosts in the kbucket tree
    private List<Host> knownHosts = new ArrayList<>();

    // Our host information
    private Host self;

    // interface to other nodes
    private KademliaRPC rpc;

    // The local data we are saving
    private Map<Integer, DataBlock> dataStore = new HashMap<>();

    // The size of each bucket
    private int ksize;

    public KademliaClient(int bitLen, Host self, KademliaRPC rpc, int ksize) {
        this.bitLen = bitLen;
        this.self = self;
        this.rpc = rpc;
        this.knownHosts.add(self);

        RouteNode currNode = kbucketTree = new RouteNode();

        // set up tree
        for (int i = 0; i < bitLen; i++) {
            int dir = getBit(self.key, i);
            RouteNode nextNode = new RouteNode();

            //check branch
            if(dir == 0) {
                currNode.setLeft(Optional.of(nextNode));
            } else {
                currNode.setRight(Optional.of(nextNode));
            }
            currNode = nextNode;
        }

        Bucket kbucket = new Bucket(ksize);
        kbucket.addNodeToBucket(self, null);
        currNode.setKbucket(Optional.of(kbucket));
    }

    /**
     * adds a host to the route tree
     * @param host the host to add
     */
    public void addHost(Host host) {
        RouteNode targetNode = getClosestBucket(host.key);
        Optional<Bucket> nodeHost = targetNode.getKbucket();

        if (!nodeHost.isPresent()) {
            Bucket kbucket = new Bucket(ksize);
            kbucket.addNodeToBucket(host, null);
            targetNode.setKbucket(Optional.of(kbucket));
            knownHosts.add(host);
            return;
        }

        // if there is no host here or the current host is greater than this host
        Bucket kbucket = nodeHost.get();
        kbucket.addNodeToBucket(host, null);
        knownHosts.add(host); // there needs to be a way to collect all these hosts for search from the buckets.
    }

    // Not yet implemented but should call find host over and over again.
    public List<Host> nodeLookup(int key) {
        // should have k elements
        return List.of();
    }

    /**
     * Handles getting data from the network. If the data is on this machine, it returns it. If not, it begins to
     * search the network calling findHost repeatedly until a result can be found or nothing.
     * @param key the key to search for
     * @return the datablock if one could be found
     */
    public Optional<DataBlock> get(int key) {
        Optional<DataBlock> dataOp = Optional.ofNullable(dataStore.get(key));
        if(dataOp.isPresent()) {
            return dataOp;
        }

        Host target = getClosestHost(key);
        Set<Host> checkedHosts = new HashSet<>();
        while(!checkedHosts.contains(target)) {
            HostSearchResult result = this.rpc.findValue(target, key);
            if (result.getData().isPresent()) {
                return result.getData();
            }

            // if there is no host and no data, then the data cannot be found
            if(result.getNextHost().isEmpty()) {
                return Optional.empty();
            }

            checkedHosts.add(target);
            target = result.getNextHost().get(0);
        }

        return Optional.empty();
    }

    public void put(int key, DataBlock data) {
        Host target = getClosestHost(key);
        // if we should store it
        if (target.equals(this.self)) {
            dataStore.put(key, data);
            return;
        }

        rpc.store(target, key, data);
    }

    public boolean hasData(int key) {
        return dataStore.containsKey(key);
    }

    public Host getClosestHost(int key) {
        return getClosestHosts(key, 1).get(0);
    }

    public List<Host> getClosestHosts(int key, int count) {
        return knownHosts.stream()
                .sorted((host1, host2) -> -((int) getDist(host1, host2)))
                .limit(count)
                .collect(Collectors.toList());
    }

    private long getDist(Host host1, Host host2) {
        long dist = host1.key ^ host2.key;
        dist += Integer.MAX_VALUE;
        return dist;
    }

    public RouteNode getClosestBucket(int key) {
        RouteNode currNode = kbucketTree;
        for (int i = 0; i < bitLen; i++) {
            int dir = getBit(key, bitLen - i - 1);
            Optional<RouteNode> nextNode;

            // check branch
            if(dir == 0) {
                nextNode = currNode.getLeft();
            } else {
                nextNode = currNode.getRight();
            }

            // if there is a next node, get the next node in the chain
            // else return this result
            if (nextNode.isPresent()) {
                currNode = nextNode.get();
            } else {
                break;
            }
        }
        return currNode;
    }

    private int getBit(int v, int id) {
        return (v >> id) & 1;
    }

    /**
     * Find the closet bucket to put new node info
     * Dealing with new node joining the network
     * @param self(local host) newHost, known node in the network and the joining node
     */
    private int findClosestBucketID(Host self, Host newHost){
        int bucketID = 0;
        for (int i = 0; i < bitLen; i++) {
            int dir1 = getBit(self.key, bitLen - i - 1);
            int dir2 = getBit(newHost.key, bitLen - i - 1);
            if(dir1 == dir2){
                continue;
            }
            else{
                bucketID = i;
                break;
            }
        }
        return bucketID;

    }

    /**
     * For new nodes joining the network. The joining node must know a node in the network
     * @param newHost,  the joining node
     */
    public void NodeJoin(Host newHost) {
        addHost(newHost);
//        int bucketID = findClosestBucketID(self, newHost);
//        System.out.println("bucketID: " + bucketID);
//        self.buckets.get(bucketID).addNodeToBucket(newHost, rpc);
    }

    @Data
    private static class RouteNode {
        Optional<Bucket> kbucket = Optional.empty(); // right now the bucket can only have 1 element. k=1 (I think)
        Optional<RouteNode> left = Optional.empty(); // 0 branch
        Optional<RouteNode> right = Optional.empty(); // 1 branch
    }
}


