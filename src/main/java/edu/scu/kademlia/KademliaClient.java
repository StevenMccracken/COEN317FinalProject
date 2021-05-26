package edu.scu.kademlia;

import lombok.Data;

import java.util.*;

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

    public KademliaClient(int bitLen, Host self, KademliaRPC rpc) {
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

        currNode.setKbucket(Optional.of(self));
    }

    /**
     * adds a host to the route tree
     * @param host the host to add
     */
    public void addHost(Host host) {
        RouteNode targetNode = getClosestBucket(host.key);
        Optional<Host> nodeHost = targetNode.getKbucket();

        if (!nodeHost.isPresent()) {
            targetNode.setKbucket(Optional.of(host));
            knownHosts.add(host);
            return;
        }

        // if there is no host here or the current host is greater than this host
        if (nodeHost.get().key > host.key) {
            knownHosts.remove(nodeHost.get());
            targetNode.setKbucket(Optional.of(host));
            knownHosts.add(host);
        }
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
            if(!result.getNextHost().isPresent()) {
                return Optional.empty();
            }

            checkedHosts.add(target);
            target = result.getNextHost().get();
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
        Host best = self;
        // pretend this is an unsigned
        long bestDist = Integer.MAX_VALUE * 2L;
        for(Host host : knownHosts) {
            long dist = host.key ^ key;
            dist += Integer.MAX_VALUE;
            if (dist < bestDist) {
                best = host;
                bestDist = dist;
            }
        }

        return best;
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
        int bucketID = findClosestBucketID(self, newHost);
        System.out.println("bucketID: " + bucketID);
        self.buckets.get(bucketID).addNodeToBucket(newHost, rpc);
    }

    @Data
    private static class RouteNode {
        Optional<Host> kbucket = Optional.empty(); // right now the bucket can only have 1 element. k=1 (I think)
        Optional<RouteNode> left = Optional.empty(); // 0 branch
        Optional<RouteNode> right = Optional.empty(); // 1 branch
    }
}


