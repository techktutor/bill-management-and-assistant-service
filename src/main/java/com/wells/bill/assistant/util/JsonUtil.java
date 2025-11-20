package com.wells.bill.assistant.util;

import com.fasterxml.jackson.databind.ObjectMapper;


public class JsonUtil {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String toJson(Object obj) {
        try { return mapper.writeValueAsString(obj); }
        catch (Exception e) { throw new RuntimeException(e); }
    }


    public static <T> T fromJson(String json, Class<T> cls) {
        try { return mapper.readValue(json, cls); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
