package raft.maple.core;

import raft.maple.proto.Log;

/**
 * @author yilin wang <yiliwang@student.unimelb.edu.au>
 * @version 1.0
 * @since 1.0
 * <p>
 * The University of Melbourne COMP900020 Assignment created in 2021/04:
 * core.StateMachine define the common operations for a statemachine.
 */
public interface StateMachine {

    byte[] get(byte[] key);

    void put(byte[] key, byte[] value);

    void delete(byte[] key);

    void applyLog(Log log);

    void print();

}
