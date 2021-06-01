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
        return this.handler.findNode(key);
    }

    @Override
    public HostSearchResult findValue(long key) throws RemoteException {
        return this.handler.findValue(key);
    }

    @Override
    public void store(long key, DataBlock data) throws RemoteException {
        this.handler.store(key, data);
    }

    @Override
    public boolean ping() throws RemoteException {
        return this.handler.ping();
    }
}
