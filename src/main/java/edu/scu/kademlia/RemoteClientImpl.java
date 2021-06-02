package edu.scu.kademlia;

import java.rmi.RemoteException;
import java.util.List;

public class RemoteClientImpl implements RemoteClient {
    private final Client handler;
    public RemoteClientImpl(Client handler) {
        this.handler = handler;
    }

    @Override
    public List<Host> findNode(long key) throws RemoteException {
        System.out.println("Received remote findNode");
        return this.handler.findNode(key);
    }

    @Override
    public HostSearchResult findValue(long key) throws RemoteException {
        System.out.println("Received remote findValue");
        return this.handler.findValue(key);
    }

    @Override
    public void store(long key, DataBlock data) throws RemoteException {
        System.out.println("Received remote store");
        this.handler.store(key, data);
    }

    @Override
    public boolean ping() throws RemoteException {
        System.out.println("Received remote ping");
        return this.handler.ping();
    }
}
