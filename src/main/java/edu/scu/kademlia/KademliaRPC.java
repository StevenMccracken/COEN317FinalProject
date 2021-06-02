package edu.scu.kademlia;

import java.rmi.ConnectException;
import java.util.List;

public interface KademliaRPC {

    /**
     * @param host The host to send this RPC to
     * @param key  The key we would like to find the host for
     * @return A list of up to k nodes that are closest to the target key
     */
    List<Host> findNode(Host host, long key) throws ConnectException;

    /**
     * @param host The host to send this RPC to
     * @param key  The key we would like to find the host for
     * @return Either the next host to contact or the requested data
     */
    HostSearchResult findValue(Host host, long key) throws ConnectException;

    void store(Host host, long key, DataBlock data) throws ConnectException;

    boolean ping(Host host);
}
