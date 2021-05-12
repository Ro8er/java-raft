package raft.maple.core;

import raft.maple.proto.Log;

public interface StateMachine {

    byte[] get(byte[] key);

    void put(byte[] key, byte[] value);

    void delete(byte[] key);

    void applyLog(Log log);

    void print();

}
