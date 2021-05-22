package edu.scu.kademlia;

interface KademliaRPC {
    /**
     * @param host The host to send this RPC to
     * @param key  The key we would like to find the host for
     * @return Either the next host to contact or the requested data
     */
    Host findHost(Host host, int key);

    HostSearchResult findValue(Host host, int key);

    void store(Host host, int key, DataBlock data);

    boolean ping(Host host);
}
