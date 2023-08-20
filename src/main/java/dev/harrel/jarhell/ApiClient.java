package dev.harrel.jarhell;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.model.central.ArtifactDoc;
import dev.harrel.jarhell.model.central.SelectResponse;
import dev.harrel.jarhell.model.central.VersionDoc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;
import java.util.stream.Collectors;

public class ApiClient {
    private static final String SELECT_URL = "https://search.maven.org/solrsearch/select";
    private static final String CONTENT_URL = "https://search.maven.org/remotecontent";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ApiClient(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public String fetchLatestVersion(String groupId, String artifactId) {
        String query = "?q=g:%s+AND+a:%s".formatted(groupId, artifactId);
        SelectResponse<ArtifactDoc> selectResponse = fetch(SELECT_URL + query, new TypeReference<>() {
        });
        if (selectResponse.response().numFound() < 1) {
            throw new IllegalArgumentException("Artifact couldn't be found: %s:%s".formatted(groupId, artifactId));
        }
        return selectResponse.response().docs().get(0).latestVersion();
    }

    public Set<String> fetchAvailableFiles(Gav gav) {
        String query = "?q=g:%s+AND+a:%s+AND+v:%s".formatted(gav.groupId(), gav.artifactId(), gav.version());
        SelectResponse<VersionDoc> selectResponse = fetch(SELECT_URL + query, new TypeReference<>() {
        });
        if (selectResponse.response().numFound() < 1) {
            throw new IllegalArgumentException("Artifact couldn't be found: " + gav);
        }
        return selectResponse.response().docs().get(0).ec().stream()
                .filter(f -> f.startsWith("."))
                .filter(f -> !f.endsWith("sha256") && !f.endsWith("sha512"))
                .map(f -> f.substring(1))
                .collect(Collectors.toSet());
    }

    private <T> T fetch(String url, TypeReference<T> type) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalArgumentException("HTTP call failed [%s] for url [%s]".formatted(response.statusCode(), url));
            }
            return objectMapper.readValue(response.body(), type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException(e);
        }
    }

    public static String createFileUrl(Gav gav, String fileExtension) {
        String groupPath = gav.groupId().replace('.', '/');
        String fileName = "%s-%s.%s".formatted(gav.artifactId(), gav.version(), fileExtension);
        String query = "?filepath=%s/%s/%s/%s".formatted(groupPath, gav.artifactId(), gav.version(), fileName);
        return CONTENT_URL + query;
    }
}
