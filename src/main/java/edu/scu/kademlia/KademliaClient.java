package edu.scu.kademlia;

import lombok.Getter;

import java.rmi.ConnectException;
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

    private Set<Long> recentStores = new HashSet<>();

//    private final RemoteClientImpl remoteClient;

    public KademliaClient(int bitLen, Host self, KademliaRPC rpc, int ksize, boolean useRemoteClient) {
        this.bitLen = bitLen;
        this.self = self;
        this.rpc = rpc;
        this.ksize = ksize;
        this.remoteClient = useRemoteClient ? new RemoteClientImpl(this) : null;

        kbucketTree = new RouteNode();
        Bucket baseBucket = new Bucket(ksize, rpc);
        kbucketTree.setKbucket(baseBucket);
        allBuckets.add(baseBucket);
        addHost(self);

        if (useRemoteClient) {
            try {
                final RemoteClient stub = (RemoteClient) UnicastRemoteObject.exportObject(this.remoteClient, self.port);
                final Registry registry = LocateRegistry.getRegistry();
                registry.bind(Long.toString(self.key), stub);
            } catch (RemoteException | AlreadyBoundException exception) {
                exception.printStackTrace();
            }
        }
    }

    public void start(Host introducer) {
        if (introducer == null) {
            return;
        }
        addHost(introducer);

        List<Host> others = nodeLookup(self.key, true);

        for (Host other : others) {
            addHost(other);
        }
    }

    public void removeHost(Host host) {
        RouteNode targetNode = getClosestNode(host.getKey());
        Bucket kbucket = targetNode.getKbucket().get();
        kbucket.removeHost(host);
    }

    /**
     * adds a host to the route tree
     *
     * @param host the host to add
     */
    public void addHost(Host host) {
        RouteNode targetNode = getClosestNode(host.getKey());
        Bucket kbucket = targetNode.getKbucket().get();
        boolean inOwnBucket = kbucket.contains(self);
        boolean success = kbucket.addHost(host);

        // if we failed to insert and the bucket contains this node
        if (success) {
            return;
        }

        if (!inOwnBucket) {
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
        for (Host host : oldBucket.getNodesInBucket()) {
            Bucket kbucket = getClosestBucket(host.getKey());
            kbucket.addHost(host);
        }
    }

    public List<Host> nodeLookup(long key, boolean isNew) {
        // Pretend that alpha = 1
        Deque<Host> toCheck = new ArrayDeque<>(getClosestHosts(key, ksize, !isNew));
        Set<Host> checkedHosts = new HashSet<>();
        checkedHosts.add(self);
        while (!toCheck.isEmpty()) {
            Host target = toCheck.removeFirst();
            if (checkedHosts.contains(target)) {
                continue;
            }


            List<Host> otherNodes;
            try {
                otherNodes = rpc.findNode(target, key, isNew);
                addHost(target);
            } catch (ConnectException e) {
                removeHost(target);
                otherNodes = List.of();
            }
            checkedHosts.add(target);

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
     *
     * @param key the key to search for
     * @return the datablock if one could be found
     */
    public DataBlock get(long key) {
        final DataBlock data = dataStore.get(key);
        if (data != null) {
            return data;
        }

        Deque<Host> toCheck = new ArrayDeque<>(getClosestHosts(key, ksize, false));
        Set<Host> checkedHosts = new HashSet<>();
        checkedHosts.add(self);
        while (!toCheck.isEmpty()) {
            Host target = toCheck.removeFirst();
            if (checkedHosts.contains(target)) {
                continue;
            }

            HostSearchResult result = null;
            checkedHosts.add(target);
            try {
                result = rpc.findValue(target, key);
                addHost(target);
            } catch (ConnectException e) {
                removeHost(target);
                continue;
            }

            if (result.getData() != null) {
                return result.getData();
            }

            for (Host other : result.getNextHost()) {
                toCheck.addLast(other);
            }
        }

        return null;
    }

    public void put(long key, DataBlock data) {
        List<Host> targets = nodeLookup(key, false); // this should get stored to k nodes but right now its just 1

        for (Host target : targets) {
            if (target.equals(this.self)) {
                store(key, data);
            } else {
                try {
                    rpc.store(target, key, data);
                } catch (ConnectException e) {
                    removeHost(target);
                }
            }
        }
    }

    public void replicateClosest(Host target) {
        for(var entry : dataStore.entrySet()) {
            Host closest = getClosestHosts(entry.getKey(), 2, true)
                    .stream()
                    .filter(host -> !host.equals(target))
                    .findFirst()
                    .get();

            if (!closest.equals(self)) {
                continue;
            }

            try {
                rpc.store(target, entry.getKey(), entry.getValue());
            } catch (ConnectException e) {
                removeHost(target);
                break;
            }
        }
    }

    public boolean hasData(long key) {
        return dataStore.containsKey(key);
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
            if (dir == 0) {
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

    public void republish() {
        Bucket selfBucket = getClosestBucket(self.getKey());
        selfBucket.refreshBucket();

        for (var entry : dataStore.entrySet()) {
            if (recentStores.contains(entry.getKey())) {
                continue;
            }
            for (Host host : selfBucket.getNodesInBucket()) {
                try {
                    rpc.store(host, entry.getKey(), entry.getValue());
                } catch (ConnectException e) {
                    removeHost(host);
                }
            }
        }

        recentStores.clear();
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
        recentStores.add(key);
        dataStore.put(key, data);
    }

    @Override
    public boolean ping() {
        return true;
    }

    public void printHosts() {
        System.out.println("[" + self.ip + "] Known Hosts:");
        for(Host host : allHosts()) {
            System.out.println("[" + self.ip + "]\t\t" + host.ip);
        }
    }

    public void printDataStore() {
        System.out.println("[" + self.ip + "] DataStore:");
        for(var entry : dataStore.entrySet()) {
            System.out.println("[" + self.ip + "]\t\t" + entry.getKey() + " -> " + entry.getValue().sampleValue);
        }
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
