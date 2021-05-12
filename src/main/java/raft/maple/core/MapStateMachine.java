package raft.maple.core;

import raft.maple.proto.Log;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MapStateMachine implements StateMachine {
    private Map<String,String> map =new ConcurrentHashMap<>();


    @Override
    public byte[] get(byte[] key) {
        String result = map.getOrDefault(new String(key), null);
        if (result == null) {
            return null;
        } else {
            return result.getBytes(StandardCharsets.UTF_8);
        }
    }

    @Override
    public void put(byte[] object, byte[] value) {
        map.put(new String(object),new String(value));

    }

    @Override
    public void delete(byte[] key) {
        map.remove(new String(key));
    }

    public void applyLog(Log log){
        Log.OperationType type= log.getCommandType();
        if (type== Log.OperationType.CREATE){
            put(log.getObject(), log.getContent());
        }
        if (type== Log.OperationType.UPDATE){
            put(log.getObject(), log.getContent());
        }
        if (type== Log.OperationType.DELETE){
            delete(log.getObject());
        }
    }

    @Override
    public void print() {
        System.out.println("statemachine snapshot: "+map.toString());
    }

    @Override
    public String toString(){return map.toString();}
}
