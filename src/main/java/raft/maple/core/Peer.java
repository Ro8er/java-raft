package raft.maple.core;

import raft.maple.socket.RPClient;

/**
 * @author yilin wang <yiliwang@student.unimelb.edu.au>
 * @version 1.1
 * @since 1.1
 * <p>
 * The University of Melbourne COMP900020 Assignment created in 2021/04:
 * core.Peer encapsulates the peer's id, nextIndex, matchIndex for leader's peerMap
 * so leader's can send appendEntriesRequest tailoring the situation of that peer.
 *
 * Note when we say peer, it means all the followers of the leader
 */
public class Peer {

    /**
     * Peer's id
     *
     * @since 1.0
     */
    private int id;

    /**
     * leader should send the log at this position to this peer next time
     *
     * @since 1.0
     */
    private long nextIndex;  //Log index that should send to this peer net time

    /**
     * the index of log that this peer matches with leader.
     *
     * @since 1.0
     */
    private long matchIndex;   //highest index the peer replicate

    /**
     * Not used in current version ,may be used in the future design if refactor the I/O module
     *
     * @since 1.0
     */
    private RPClient rpClient; // may be used in the future design

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
