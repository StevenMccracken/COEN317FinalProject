package edu.scu.kademlia;

import lombok.Getter;
import lombok.Setter;

public class Host {
    @Getter
    final String ip;

    @Getter
    private long key;

    // udp port
    @Getter
    final int port;

    //time stamp for each node, RPC can update this value
    @Getter @Setter
    private long mostRecentSeen;

    public Host(String ip, long key, int port) {
        this.ip = ip;
        this.key = key;
        this.port = port;
        this.mostRecentSeen = System.currentTimeMillis();
    }
}
