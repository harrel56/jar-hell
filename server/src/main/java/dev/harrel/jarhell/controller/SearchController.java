package dev.harrel.jarhell.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.harrel.jarhell.model.central.SelectResponse;
import io.avaje.http.api.Controller;
import io.avaje.http.api.Get;
import io.avaje.http.api.QueryParam;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller("/api/v1/search")
class SearchController {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    SearchController(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Get
    List<Artifact> search(@QueryParam String query) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            URI uri = URI.create("https://search.maven.org/solrsearch/select?q=" + encodedQuery);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalArgumentException("HTTP call failed [%s] for url [%s]".formatted(response.statusCode(), uri));
            }
            SelectResponse<Artifact> responseDoc = objectMapper.readValue(response.body(), new TypeReference<>() {});
            return responseDoc.response().docs();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException(e);
        }
    }

    record Artifact(String g, String a) {}
}
