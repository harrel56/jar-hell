package dev.harrel.jarhell.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.harrel.jarhell.Configuration;

import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class TestUtil {
    private static final ObjectMapper objectMapper = new Configuration().objectMapper()
            .configure(DeserializationFeature.USE_LONG_FOR_INTS, true);

    public static String writeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T readJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T readJson(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> List<T> iteratorToList(Iterator<T> it) {
        Stream.Builder<T> builder = Stream.builder();
        it.forEachRemaining(builder);
        return builder.build().toList();
    }
}
