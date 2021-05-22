package edu.scu.kademlia;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.io.InvalidClassException;
import java.util.*;

class HostSearchResult {
    @Getter
    Optional<Host> nextHost = Optional.empty();
    @Getter
    Optional<DataBlock> data = Optional.empty();

    public HostSearchResult(Host nextHost) {
        this.nextHost = Optional.of(nextHost);
    }

    public HostSearchResult(DataBlock data) {
        this.data = Optional.of(data);
    }
}

interface KemdelaRPC {
    /**
     * @param host The host to send this RPC to
     * @param key The key we would like to find the host for
     * @return Either the next host to contact or the requested data
     */
     HostSearchResult findHost(Host host, int key);

//    bool hasBlock(Host host, int key);
}

public class RouteTree {

    private int bitLen;

    private RouteNode root;

    private Host self;

    private KemdelaRPC rpc;

    private Map<Integer, DataBlock> dataStore = new HashMap<>();

    public RouteTree(int bitLen, Host self, KemdelaRPC rpc) {
        this.bitLen = bitLen;
        this.self = self;
        this.rpc = rpc;
        RouteNode currNode = root = new RouteNode();

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

        currNode.setHost(Optional.of(self));
    }

    /**
     * adds a host to the route tree
     * @param host the host to add
     */
    public void addHost(Host host) {
        RouteNode targetNode = getClosest(host.key);
        Optional<Host> nodeHost = targetNode.getHost();

        // if there is no host here or the current host is greater than this host
        if (!nodeHost.isPresent() || nodeHost.get().key > host.key) {
            targetNode.setHost(Optional.of(host));
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

        RouteNode target = getClosest(key);

        if (!target.getHost().isPresent()) {
            throw new UnsupportedOperationException("UNSURE WHAT TO DO");
        }

        Set<Host> checkedHosts = new HashSet<>();
        Host toSearch = target.getHost().get();
        while(!checkedHosts.contains(toSearch)) {
            HostSearchResult result = this.rpc.findHost(toSearch, key);
            if (result.getData().isPresent()) {
                return result.getData();
            }

            if(!result.getNextHost().isPresent()) {
                throw new UnsupportedOperationException("Result must have host or data!");
            }

            checkedHosts.add(toSearch);
            toSearch = result.getNextHost().get();
        }

        return Optional.empty();
    }

    public void put(int key, DataBlock data) {
        
    }

    private RouteNode getClosest(int key) {
        RouteNode currNode = root;
        for (int i = 0; i < bitLen; i++) {
            int dir = getBit(key, i);
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
}

@AllArgsConstructor
class Host {
    String ip;
    int key;
}

@Data
class RouteNode {
    Optional<Host> host = Optional.empty();
    Optional<RouteNode> left = Optional.empty(); // 0 branch
    Optional<RouteNode> right = Optional.empty(); // 1 branch
}

@Data
class DataBlock {

}