package dev.harrel.jarhell.analyze;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.model.central.ArtifactDoc;
import dev.harrel.jarhell.model.central.MavenMetadata;
import dev.harrel.jarhell.model.central.SelectResponse;
import dev.harrel.jarhell.model.central.VersionDoc;
import io.avaje.config.Config;

import javax.inject.Singleton;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Singleton
class ApiClient {
    private static final String SEARCH_URL = Config.get("maven.search-url");
    private static final String CONTENT_URL = Config.get("maven.content-url");

    private final ObjectMapper objectMapper;
    private final XmlMapper xmlMapper;
    private final HttpClient httpClient;

    ApiClient(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.xmlMapper = new XmlMapper();
        this.xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.httpClient = httpClient;
    }

    public List<String> fetchArtifactVersions(String groupId, String artifactId) {
        String groupPath = groupId.replace('.', '/');
        String query = "?filepath=%s/%s/maven-metadata.xml".formatted(groupPath, artifactId);
        MavenMetadata metadata = fetch(CONTENT_URL + query, xmlMapper, new TypeReference<>() {});
        return metadata.versioning().versions();
    }

    public String fetchLatestVersion(String groupId, String artifactId) {
        String query = "?q=g:%s+AND+a:%s".formatted(groupId, artifactId);
        SelectResponse<ArtifactDoc> selectResponse = fetch(SEARCH_URL + query, objectMapper, new TypeReference<>() {});
        if (selectResponse.response().numFound() < 1) {
            throw new ArtifactNotFoundException("%s:%s".formatted(groupId, artifactId));
        }
        return selectResponse.response().docs().getFirst().latestVersion();
    }

    public boolean checkIfArtifactExists(Gav gav) {
        String query = "?q=g:%s+AND+a:%s+AND+v:%s".formatted(gav.groupId(), gav.artifactId(), gav.version());
        SelectResponse<VersionDoc> selectResponse = fetch(SEARCH_URL + query, objectMapper, new TypeReference<>() {});
        return selectResponse.response().numFound() > 0;
    }

    public FilesInfo fetchFilesInfo(Gav gav) {
        String query = "?q=g:%s+AND+a:%s+AND+v:%s".formatted(gav.groupId(), gav.artifactId(), gav.version());
        SelectResponse<VersionDoc> selectResponse = fetch(SEARCH_URL + query, objectMapper, new TypeReference<>() {});
        if (selectResponse.response().numFound() < 1) {
            throw new ArtifactNotFoundException(gav.toString());
        }
        List<String> ec = selectResponse.response().docs().getFirst().ec();
        Set<String> extensions = ec.stream()
                .filter(f -> f.startsWith("."))
                .map(f -> f.substring(1))
                .collect(Collectors.toSet());
        Set<String> classifiers = ec.stream()
                .filter(f -> f.startsWith("-"))
                .map(f -> f.substring(1, f.indexOf(".")))
                .collect(Collectors.toSet());
        return new FilesInfo(extensions, classifiers);
    }

    private <T> T fetch(String url, ObjectMapper objectMapper, TypeReference<T> type) {
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
        StringJoiner joiner = new StringJoiner("-")
                .add(gav.artifactId())
                .add(gav.version());
        if (gav.classifier() != null) {
            joiner.add(gav.classifier());
        }
        String fileName = "%s.%s".formatted(joiner, fileExtension);
        String query = "?filepath=%s/%s/%s/%s".formatted(groupPath, gav.artifactId(), gav.version(), fileName);
        return CONTENT_URL + query;
    }
}
