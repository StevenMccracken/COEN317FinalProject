package edu.scu.kademlia;

import java.util.List;

interface KademliaRPC {

    /**
     * @param host The host to send this RPC to
     * @param key  The key we would like to find the host for
     * @return A list of up to k nodes that are closest to the target key
     */
    List<Host> findNode(Host host, int key);

    /**
     * @param host The host to send this RPC to
     * @param key  The key we would like to find the host for
     * @return Either the next host to contact or the requested data
     */
    HostSearchResult findValue(Host host, int key);

    void store(Host host, int key, DataBlock data);

    boolean ping(Host host);
}
