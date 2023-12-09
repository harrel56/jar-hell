package dev.harrel.jarhell.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.harrel.jarhell.error.BadRequestException;
import dev.harrel.jarhell.model.central.SelectResponse;
import io.avaje.config.Config;
import io.avaje.http.api.Controller;
import io.avaje.http.api.Get;
import io.avaje.http.api.QueryParam;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Controller("/api/v1/search")
class SearchController {
    private static final String SEARCH_URL = Config.get("maven.search-url");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    SearchController(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Get
    List<Artifact> search(@QueryParam String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        try {
            URI uri = URI.create(SEARCH_URL + "?q=" + createQueryString(query));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new BadRequestException("HTTP call failed [%s] for url [%s]".formatted(response.statusCode(), uri));
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

    private String createQueryString(String input) {
        if (input.contains(":")) {
            String[] split = input.split(":", -1);
            return "g:%s*+AND+a:%s*".formatted(split[0], split[1]);
        } else {
            return "g:%1$s*+OR+a:%1$s*".formatted(input);
        }
    }

    record Artifact(String g, String a) {}
}
