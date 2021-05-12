package raft.maple.Util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonTool {
    private static ObjectMapper mapper = new ObjectMapper();

    public static String ObjectToJsonString(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <T> T JsonStringToObject(String str, Class<T> valueType) {
        try {
            return mapper.readValue(str, valueType);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }

    }
}




