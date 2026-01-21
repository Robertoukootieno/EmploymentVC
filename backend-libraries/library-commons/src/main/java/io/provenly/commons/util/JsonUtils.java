package io.provenly.commons.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Utility class for JSON serialization and deserialization.
 * Provides a centralized ObjectMapper configuration.
 */
@Slf4j
public final class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

    private JsonUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Create and configure the ObjectMapper.
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Register Java 8 date/time module
        mapper.registerModule(new JavaTimeModule());
        
        // Configure serialization
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        // Configure deserialization
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        
        return mapper;
    }

    /**
     * Get the configured ObjectMapper instance.
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    /**
     * Convert object to JSON string.
     */
    public static String toJson(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Error converting object to JSON", e);
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }

    /**
     * Convert object to pretty-printed JSON string.
     */
    public static String toPrettyJson(Object object) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Error converting object to pretty JSON", e);
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }

    /**
     * Convert JSON string to object.
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON to object", e);
            throw new RuntimeException("Failed to deserialize JSON to object", e);
        }
    }

    /**
     * Convert JSON string to object using TypeReference.
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON to object", e);
            throw new RuntimeException("Failed to deserialize JSON to object", e);
        }
    }

    /**
     * Convert JSON InputStream to object.
     */
    public static <T> T fromJson(InputStream inputStream, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(inputStream, clazz);
        } catch (IOException e) {
            log.error("Error parsing JSON stream to object", e);
            throw new RuntimeException("Failed to deserialize JSON stream to object", e);
        }
    }

    /**
     * Convert object to Map.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(Object object) {
        return OBJECT_MAPPER.convertValue(object, Map.class);
    }

    /**
     * Convert Map to object.
     */
    public static <T> T fromMap(Map<String, Object> map, Class<T> clazz) {
        return OBJECT_MAPPER.convertValue(map, clazz);
    }

    /**
     * Deep clone an object using JSON serialization.
     */
    public static <T> T clone(T object, Class<T> clazz) {
        try {
            String json = toJson(object);
            return fromJson(json, clazz);
        } catch (Exception e) {
            log.error("Error cloning object", e);
            throw new RuntimeException("Failed to clone object", e);
        }
    }

    /**
     * Check if a string is valid JSON.
     */
    public static boolean isValidJson(String json) {
        try {
            OBJECT_MAPPER.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }
}

