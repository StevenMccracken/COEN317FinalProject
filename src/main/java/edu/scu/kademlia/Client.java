package edu.scu.kademlia;

import java.rmi.RemoteException;
import java.util.List;

public interface Client {
    /**
     * @param key  The key we would like to find the host for
     * @return A list of up to k nodes that are closest to the target key
     */
    List<Host> findNode(long key) throws RemoteException;

    /**
     * @param key  The key we would like to find the host for
     * @return Either the next host to contact or the requested data
     */
    HostSearchResult findValue(long key) throws RemoteException;

    void store(long key, DataBlock data) throws RemoteException;

    boolean ping() throws RemoteException;
}
