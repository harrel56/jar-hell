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
import java.util.StringJoiner;
import java.util.regex.Pattern;

@Controller("/api/v1/search")
class SearchController {
    private static final String SEARCH_URL = Config.get("maven.search-url");
    private static final Pattern SANITIZATION_PATTERN = Pattern.compile("[^\\w\\.-]");

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
        String queryString = createQueryString(query);
        if (queryString.isEmpty()) {
            return List.of();
        }

        try {
            URI uri = URI.create(SEARCH_URL + "?q=" + queryString);
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
            StringJoiner joiner = new StringJoiner("+AND+");
            String groupToken = sanitize(split[0]);
            String artifactToken = sanitize(split[1]);
            if (!groupToken.isEmpty()) {
                joiner.add(groupToken);
            }
            if (!artifactToken.isEmpty()) {
                joiner.add(artifactToken);
            }
            return joiner.toString();
        } else {
            String token = sanitize(input);
            return token.isEmpty() ? "" : "g:%1$s*+OR+a:%1$s*".formatted(token);
        }
    }

    private String sanitize(String input) {
        return SANITIZATION_PATTERN.matcher(input).replaceAll("");
    }

    record Artifact(String g, String a) {}
}
