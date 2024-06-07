package com.tibame.utils.basic;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;


// 嘗試復刻JSONUtil的toJsonStr跟toBean功能
public class JSONUtil {
    private final static ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer());
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
        objectMapper.registerModule(module);
    }

    // 指定私有類別的序列化跟反序列化設定
    private static class LocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {
        protected LocalDateTimeDeserializer() {
            super(LocalDateTime.class);
        }

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            long timestamp = node.asLong();
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC);
        }
    }

    private static class LocalDateTimeSerializer extends StdSerializer<LocalDateTime> {
        protected LocalDateTimeSerializer() {
            super(LocalDateTime.class);
        }

        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            long timestamp = value.toInstant(ZoneOffset.UTC).toEpochMilli();
            gen.writeNumber(timestamp);
        }
    }


    /**
     * 將傳入的物件轉換成Json字串
     * @param obj 要被轉換的物件
     * @return 轉換後的Json字串
     */
    public static String toJsonStr(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 將Json字串轉為物件
     * @param json Json格式的字串
     * @param type 指定要轉換的型態
     * @return 轉換後的物件
     * @param <R> 回傳的型態 根據type決定
     */
    public static <R> R toBean(String json, Class<R> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 將物件轉為另一個型態的物件 (實際上應該是LinkedHashMap物件)
     * @param data LinkedHashMap物件
     * @param type 指定要轉換的型態
     * @return 轉換後的物件
     * @param <R> 回傳的型態 根據type決定
     */
    public static <R> R toBean(Object data, Class<R> type) {
        return objectMapper.convertValue(data, type);
    }
}