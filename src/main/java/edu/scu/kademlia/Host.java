package edu.scu.kademlia;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

public class Host implements Serializable {
    @Getter
    final String ip;

    @Getter
    final long key;

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
