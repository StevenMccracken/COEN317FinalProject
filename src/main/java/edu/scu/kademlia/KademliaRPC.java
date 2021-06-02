package edu.scu.kademlia;

import java.util.List;

public interface KademliaRPC {

    /**
     * @param host The host to send this RPC to
     * @param key  The key we would like to find the host for
     * @return A list of up to k nodes that are closest to the target key
     */
    List<Host> findNode(Host host, long key);

    /**
     * @param host The host to send this RPC to
     * @param key  The key we would like to find the host for
     * @return Either the next host to contact or the requested data
     */
    HostSearchResult findValue(Host host, long key);

    void store(Host host, long key, DataBlock data);

    boolean ping(Host host);
}
