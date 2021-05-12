package raft.maple.core;

import raft.maple.socket.RPClient;

public class Peer {
    private int id;
    private long nextIndex;  //Log index that should send to this peer net time
    private long matchIndex;   //highest index the peer replicate
    private RPClient rpClient;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getNextIndex() {
        return nextIndex;
    }

    public void setNextIndex(long nextIndex) {
        this.nextIndex = nextIndex;
    }

    public long getMatchIndex() {
        return matchIndex;
    }

    public void setMatchIndex(long matchIndex) {
        this.matchIndex = matchIndex;
    }

    public RPClient getRpClient() {
        return rpClient;
    }

    public void setRpClient(RPClient rpClient) {
        this.rpClient = rpClient;
    }
}
