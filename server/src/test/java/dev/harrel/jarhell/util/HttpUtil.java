package dev.harrel.jarhell.util;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static java.net.http.HttpResponse.BodySubscribers;

public class HttpUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static HttpRequest.BodyPublisher jsonPublisher(Object obj) {
        try {
            String value = objectMapper.writeValueAsString(obj);
            return HttpRequest.BodyPublishers.ofString(value);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> HttpResponse.BodyHandler<T> jsonHandler(Class<T> clazz) {
        return responseInfo -> BodySubscribers.mapping(BodySubscribers.ofByteArray(), bytes -> {
            try {
                return objectMapper.readValue(bytes, clazz);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
}
