package edu.scu.kademlia;

import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

public class KademliaClient implements Client {

    // The bits of the key to use
    private int bitLen;

    // The root of our route tree. Each node contains a kbucket.
    private RouteNode kbucketTree;

    // The hosts in our address book. Points to hosts in the kbucket tree
    @Getter
    private Set<Bucket> allBuckets = new HashSet<>();

    // Our host information
    private Host self;

    // interface to other nodes
    private KademliaRPC rpc;

    // The local data we are saving
    @Getter
    private Map<Long, DataBlock> dataStore = new HashMap<>();

    // The size of each bucket
    private int ksize;

//    private final RemoteClientImpl remoteClient;

    public KademliaClient(int bitLen, Host self, KademliaRPC rpc, int ksize) {
        this.bitLen = bitLen;
        this.self = self;
        this.rpc = rpc;
        this.ksize = ksize;
//        this.remoteClient = new RemoteClientImpl(this);

        kbucketTree = new RouteNode();
        Bucket baseBucket = new Bucket(ksize, rpc);
        kbucketTree.setKbucket(baseBucket);
        allBuckets.add(baseBucket);
        addHost(self);

//        try {
//            final RemoteClient stub = (RemoteClient) UnicastRemoteObject.exportObject(this.remoteClient, self.port);
//            final Registry registry = LocateRegistry.getRegistry();
//            registry.bind(Long.toString(self.key), stub);
//        } catch (RemoteException | AlreadyBoundException exception) {
//            exception.printStackTrace();
//        }
    }

    public Map<Long, DataBlock> getDataStore(){
        return this.dataStore;
    }

    //test only
    public Host getSelf(){
        return this.self;
    }

    public void start(Host introducer) {
        if (introducer == null) {
            return;
        }
        addHost(introducer);

        List<Host> others = nodeLookup(self.key, false);

        for (Host other : others) {
            addHost(other);
        }
    }

    /**
     * adds a host to the route tree
     * @param host the host to add
     */
    public void addHost(Host host) {
        RouteNode targetNode = getClosestNode(host.getKey());
        Bucket kbucket = targetNode.getKbucket().get();
        boolean success = kbucket.addHost(host);

        // if we failed to insert and the bucket contains this node
        if (success) {
            return;
        }

        if (!kbucket.contains(self)) {
            return;
        }

        splitNode(targetNode);
        addHost(host);
    }

    private void splitNode(RouteNode node) {
        Bucket oldBucket = node.getKbucket().get();
        node.setKbucket(null);
        allBuckets.remove(oldBucket);

        RouteNode left = new RouteNode();
        Bucket leftBucket = new Bucket(ksize, rpc);
        left.setKbucket(leftBucket);
        allBuckets.add(leftBucket);

        RouteNode right = new RouteNode();
        Bucket rightBucket = new Bucket(ksize, rpc);
        right.setKbucket(rightBucket);
        allBuckets.add(rightBucket);

        node.setLeft(left);
        node.setRight(right);

        // add the hosts to the new buckets
        for(Host host : oldBucket.getNodesInBucket()) {
            Bucket kbucket = getClosestBucket(host.getKey());
            kbucket.addHost(host);
        }
    }

    public List<Host> nodeLookup(long key, boolean matchSelf) {
        // Pretend that alpha = 1
        Deque<Host> toCheck = new ArrayDeque<>(getClosestHosts(key, ksize, matchSelf));
        Set<Host> checkedHosts = new HashSet<>();
        checkedHosts.add(self);
        while(!toCheck.isEmpty()) {
            Host target = toCheck.removeFirst();
            if (checkedHosts.contains(target)) {
                continue;
            }

            List<Host> otherNodes = rpc.findNode(target, key);
            checkedHosts.add(target);
            addHost(target);

            for (Host other : otherNodes) {
                toCheck.addLast(other);
            }
        }

        return checkedHosts.stream()
                .sorted((host1, host2) -> (int) (getDist(key, host1.getKey()) - getDist(key, host2.getKey())))
                .limit(ksize)
                .collect(Collectors.toList());
    }

    /**
     * Handles getting data from the network. If the data is on this machine, it returns it. If not, it begins to
     * search the network calling findHost repeatedly until a result can be found or nothing.
     * @param key the key to search for
     * @return the datablock if one could be found
     */
    public DataBlock get(long key) {
        final DataBlock data = dataStore.get(key);
        if(data != null) {
            return data;
        }

        Host target = getClosestHost(key, true);
        Set<Host> checkedHosts = new HashSet<>();
        while(!checkedHosts.contains(target)) {
            HostSearchResult result = this.rpc.findValue(target, key);
            if (result.getData() != null) {
                return result.getData();
            }

            // if there is no host and no data, then the data cannot be found
            if(result.getNextHost().isEmpty()) {
                return null;
            }

            checkedHosts.add(target);
            target = result.getNextHost().get(0);
        }

        return null;
    }

    public void put(long key, DataBlock data) {
        Host target = nodeLookup(key, true).get(0); // this should get stored to k nodes but right now its just 1
        // if we should store it
        if (target.equals(this.self)) {
            dataStore.put(key, data);
            return;
        }

        rpc.store(target, key, data);
    }

    public boolean hasData(long key) {
        return dataStore.containsKey(key);
    }

    public Host getClosestHost(long key, boolean matchSelf) {
        return getClosestHosts(key, 1, matchSelf).get(0);
    }

    public List<Host> allHosts() {
        return allBuckets.stream()
                .flatMap(bucket -> bucket.getNodesInBucket().stream())
                .collect(Collectors.toList());
    }

    public List<Host> getClosestHosts(long key, int count, boolean matchSelf) {
        return allHosts().stream()
                .sorted((host1, host2) -> (int) (getDist(key, host1.getKey()) - getDist(key, host2.getKey())))
                .filter(host -> !(host.equals(self) && !matchSelf))
                .limit(count)
                .collect(Collectors.toList());
    }

    private long getDist(long host1, long host2) {
        long dist = host1 ^ host2;
        dist += Integer.MAX_VALUE;
        return dist;
    }

    public Bucket getClosestBucket(long key) {
        return getClosestNode(key).getKbucket().get();
    }

    public RouteNode getClosestNode(long key) {
        RouteNode currNode = kbucketTree;
        for (int i = 0; i < bitLen; i++) {
            long dir = getBit(key, bitLen - i - 1);
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

    private long getBit(long v, int id) {
        return (v >> id) & 1;
    }

    @Override
    public List<Host> findNode(long key) {
        return getClosestHosts(key, ksize, true);
    }

    @Override
    public HostSearchResult findValue(long key) {
        if (this.hasData(key)) {
            return new HostSearchResult(this.get(key));
        }
        return new HostSearchResult(this.getClosestHosts(key, ksize, true));
    }

    @Override
    public void store(long key, DataBlock data) {
        this.put(key, data);
    }

    //test only
    public void dummyStore(long key, DataBlock data) {
        this.dataStore.put(key, data);
    }

    @Override
    public boolean ping() {
        return true;
    }

    private static class RouteNode {
        @Getter
        Optional<Bucket> kbucket = Optional.empty(); // right now the bucket can only have 1 element. k=1 (I think)
        @Getter
        Optional<RouteNode> left = Optional.empty(); // 0 branch
        @Getter
        Optional<RouteNode> right = Optional.empty(); // 1 branch

        public void setKbucket(Bucket kbucket) {
            this.kbucket = Optional.ofNullable(kbucket);
        }

        public void setLeft(RouteNode left) {
            this.left = Optional.ofNullable(left);
        }

        public void setRight(RouteNode right) {
            this.right = Optional.ofNullable(right);
        }
    }
}


