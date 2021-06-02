package edu.scu.kademlia;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.List;

public final class KademliaRPCImpl implements KademliaRPC {
    private final RemoteClient getStub(Host host) {
        try {
            final Registry registry = LocateRegistry.getRegistry(null);
            return (RemoteClient)registry.lookup(Long.toString(host.key));
        } catch (RemoteException exception) {
            System.err.println("Unable to get registry from LocateRegistry while getting stub.");
        } catch (NotBoundException exception) {
            System.err.println("Unable to lookup implementation in registry.");
        }
        return null;
    }

    @Override
    public List<Host> findNode(Host host, long key) {
        try {
            return getStub(host).findNode(key);
        } catch (RemoteException exception) {
            exception.printStackTrace();
            return new ArrayList<Host>();
        }
    }

    @Override
    public HostSearchResult findValue(Host host, long key) {
        try {
            return getStub(host).findValue(key);
        } catch (RemoteException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    @Override
    public void store(Host host, long key, DataBlock data) {
        try {
            getStub(host).store(key, data);
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public boolean ping(Host host) {
        try {
            return getStub(host).ping();
        } catch (RemoteException exception) {
            exception.printStackTrace();
            return false;
        }
    }
}
