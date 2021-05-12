package raft.maple.core;

import raft.maple.proto.Log;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author yilin wang <yiliwang@student.unimelb.edu.au>
 * @version 1.0
 * @since 1.0
 * <p>
 * The University of Melbourne COMP900020 Assignment created in 2021/04:
 * core.MapStateMachine is an implementation for StateMachine, and it is a key-value hashmap state machine.
 */
public class MapStateMachine implements StateMachine {
    private Map<String, String> map = new ConcurrentHashMap<>();


    /**
     * Get Operation from map.
     *
     * @param key
     * @return value of the key in map, if key does not exist, return null;
     * @since 1.0
     */
    @Override
    public byte[] get(byte[] key) {
        String result = map.getOrDefault(new String(key), null);
        if (result == null) {
            return null;
        } else {
            return result.getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * put/update/create operation
     *
     * @param object the key in the map
     * @param value  the value in the map
     * @since 1.0
     */
    @Override
    public void put(byte[] object, byte[] value) {
        map.put(new String(object), new String(value));
    }

    /**
     * remove opration for map
     *
     * @param key
     * @since 1.0
     */
    @Override
    public void delete(byte[] key) {
        map.remove(new String(key));
    }

    /**
     * Apply the committed log to our state machine (map), to update the map status.
     *
     * @param log log of a writing operation
     * @since 1.0
     */
    @Override
    public void applyLog(Log log) {
        Log.OperationType type = log.getCommandType();
        if (type == Log.OperationType.CREATE) {
            put(log.getObject(), log.getContent());
        }
        if (type == Log.OperationType.UPDATE) {
            put(log.getObject(), log.getContent());
        }
        if (type == Log.OperationType.DELETE) {
            delete(log.getObject());
        }
    }

    /**
     * print the current map status
     *
     * @since 1.0
     */
    @Override
    public void print() {
        System.out.println("statemachine snapshot: " + map.toString());
    }

    /**
     * print the map status
     *
     * @return map status
     * @since 1.0
     */
    @Override
    public String toString() {
        return map.toString();
    }
}
