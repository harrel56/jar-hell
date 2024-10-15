package dev.harrel.jarhell;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import dev.harrel.jarhell.analyze.ArtifactNotFoundException;
import dev.harrel.jarhell.analyze.FilesInfo;
import dev.harrel.jarhell.error.BadRequestException;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.model.central.ArtifactDoc;
import dev.harrel.jarhell.model.central.MavenMetadata;
import dev.harrel.jarhell.model.central.SelectResponse;
import dev.harrel.jarhell.model.central.VersionDoc;
import io.avaje.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Singleton
public class MavenApiClient {
    private static final Logger logger = LoggerFactory.getLogger(MavenApiClient.class);

    private static final String SEARCH_URL = Config.get("maven.search-url");
    private static final String CONTENT_URL = Config.get("maven.repo-url");

    private static final Pattern SANITIZATION_PATTERN = Pattern.compile("[^\\w\\.-]");

    private final ObjectMapper objectMapper;
    private final XmlMapper xmlMapper;
    private final HttpClient httpClient;

    MavenApiClient(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.xmlMapper = new XmlMapper();
        this.xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.httpClient = httpClient;
    }

    public List<SolrArtifact> queryMavenSolr(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String queryString = createQueryString(query);
        if (queryString.isEmpty()) {
            return List.of();
        }

        SelectResponse<SolrArtifact> response = fetch(SEARCH_URL + "?q=" + queryString + "&rows=80", objectMapper, new TypeReference<>() {});
        return response.response().docs();
    }

    public List<String> fetchArtifactVersions(String groupId, String artifactId) {
        String groupPath = groupId.replace('.', '/');
        String resource = "%s/%s/maven-metadata.xml".formatted(groupPath, artifactId);
        String encodedResource = URLEncoder.encode(resource, StandardCharsets.UTF_8);
        MavenMetadata metadata = fetch(CONTENT_URL + "/" + encodedResource, xmlMapper, new TypeReference<>() {});
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
                throw new BadRequestException("HTTP call failed [%s] for url [%s]".formatted(response.statusCode(), url));
            }
            return objectMapper.readValue(response.body(), type);
        } catch (IOException e) {
            logger.error("HTTP fetch failed for url [{}]", url, e);
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
        String resource = "%s/%s/%s/%s".formatted(groupPath, gav.artifactId(), gav.version(), fileName);
        String encodedResource = URLEncoder.encode(resource, StandardCharsets.UTF_8);
        return CONTENT_URL + "/" + encodedResource;
    }

    private static String createQueryString(String input) {
        if (input.contains(":")) {
            String[] split = input.split(":", -1);
            StringJoiner joiner = new StringJoiner("+AND+");
            String groupToken = sanitizeQueryToken(split[0]);
            String artifactToken = sanitizeQueryToken(split[1]);
            if (!groupToken.isEmpty()) {
                joiner.add("g:" + groupToken + "*");
            }
            if (!artifactToken.isEmpty()) {
                joiner.add("a:" + artifactToken + "*");
            }
            return joiner.toString();
        } else {
            return sanitizeQueryToken(input); // empty string is 400, should I care?
        }
    }

    private static String sanitizeQueryToken(String input) {
        return SANITIZATION_PATTERN.matcher(input).replaceAll("");
    }

    public record SolrArtifact(String g, String a, String latestVersion) {}
}
