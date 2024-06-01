package dev.harrel.jarhell.util;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.harrel.jarhell.Configuration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static java.net.http.HttpResponse.BodySubscribers;

public class HttpUtil {
    private static final ObjectMapper objectMapper = new Configuration().objectMapper()
            .configure(DeserializationFeature.USE_LONG_FOR_INTS, true);

    public static HttpRequest.BodyPublisher jsonPublisher(Object obj) {
        try {
            String value = objectMapper.writeValueAsString(obj);
            return HttpRequest.BodyPublishers.ofString(value);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> HttpResponse.BodyHandler<T> jsonHandler(Class<T> clazz) {
        return jsonHandler(new TypeReference<>() {
            @Override
            public Type getType() {
                return clazz;
            }
        });
    }

    public static <T> HttpResponse.BodyHandler<T> jsonHandler(TypeReference<T> tr) {
        return responseInfo -> BodySubscribers.mapping(BodySubscribers.ofByteArray(), bytes -> {
            try {
                return objectMapper.readValue(bytes, tr);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
}
