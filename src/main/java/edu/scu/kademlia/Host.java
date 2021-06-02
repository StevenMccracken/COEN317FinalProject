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

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Host)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        return this.key == ((Host)obj).key;
    }

    @Override
    public int hashCode() {
        return (int)this.key;
    }
}
