package com.walter.zkt.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

/**
 * @author walter.tan
 * @date 2022/8/27
 *
 */
public class JsonUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String toJson(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

    public static <T> T fromJson(String json, Class<T> clz) throws JsonProcessingException {
        return objectMapper.readValue(json, clz);
    }

    public static byte[] toBytes(Object object) throws JsonProcessingException {
        return toJson(object).getBytes(StandardCharsets.UTF_8);
    }

    public static <T> T fromBytes(byte[] bytes, Class<T> clz) throws JsonProcessingException {
        return fromJson(new String(bytes, StandardCharsets.UTF_8), clz);
    }
}
