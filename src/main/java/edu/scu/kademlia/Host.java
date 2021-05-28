package edu.scu.kademlia;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

public class Host {
    @Getter
    String ip;

    @Getter
    private int key;

    // udp port
    @Getter
    private int port;

    //time stamp for each node, RPC can update this value
    @Getter @Setter
    private long mostRecentSeen;

    public Host(String ip, int key, int port) {
        this.ip = ip;
        this.key = key;
        this.port = port;

        this.mostRecentSeen = System.currentTimeMillis();
    }
}
