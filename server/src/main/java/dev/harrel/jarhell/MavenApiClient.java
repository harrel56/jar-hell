package dev.harrel.jarhell;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.harrel.jarhell.analyze.ArtifactNotFoundException;
import dev.harrel.jarhell.analyze.FilesInfo;
import dev.harrel.jarhell.error.BadRequestException;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.model.central.SelectResponse;
import dev.harrel.jarhell.util.ConcurrentUtil;
import io.avaje.config.Config;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

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
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Subtask;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class MavenApiClient {
    private static final String SEARCH_URL = Config.get("maven.search-url");
    private static final String CONTENT_URL = Config.get("maven.repo-url");

    private static final Pattern SANITIZATION_PATTERN = Pattern.compile("[^\\w\\.-]");
    private static final Pattern HTML_VERSIONS_PATTERN = Pattern.compile("\\d+.*/");
    private static final Pattern XML_VERSIONS_PATTERN = Pattern.compile("<version>(.+)<\\/version>");

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    MavenApiClient(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = objectMapper;
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
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            Subtask<List<String>> dirVersions = scope.fork(() -> fetchVersionsFromDir(groupId, artifactId));
            Subtask<List<String>> metadataVersions = scope.fork(() -> fetchVersionsFromMetadata(groupId, artifactId));
            ConcurrentUtil.joinScope(scope);

            List<String> result = Stream.concat(dirVersions.get().stream(), metadataVersions.get().stream())
                    .distinct()
                    .map(ComparableVersion::new)
                    .sorted()
                    .map(ComparableVersion::toString)
                    .toList();
            if (result.isEmpty()) {
                throw new IllegalArgumentException("Failed to fetch versions for %s:%s".formatted(groupId, artifactId));
            }
            return result;
        }
    }

    public boolean checkIfArtifactExists(Gav gav) {
        HttpResponse<String> res = fetchRaw(createFileUrl(gav, "pom"));
        return res.statusCode() == 200;
    }

    public FilesInfo fetchFilesInfo(Gav gav) {
        String groupPath = gav.groupId().replace('.', '/');
        String path = "%s/%s/%s/".formatted(groupPath, gav.artifactId(), gav.version());
        String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8);
        String url = CONTENT_URL + "/" + encodedPath;
        HttpResponse<String> res = fetchRaw(url);
        if (res.statusCode() >= 400) {
            throw new ArtifactNotFoundException("HTTP call failed [%s] for url [%s]".formatted(res.statusCode(), url));
        }

        String filePrefix = "%s-%s".formatted(gav.artifactId(), gav.version());
        Document doc = Jsoup.parse(res.body());
        List<String> suffixes = doc.getElementsByTag("a").stream()
                .map(el -> el.attr("href"))
                .filter(href -> href.startsWith(filePrefix))
                .map(href -> href.substring(filePrefix.length()))
                .toList();

        Set<String> extensions = suffixes.stream()
                .filter(f -> f.startsWith("."))
                .map(f -> f.substring(1))
                .collect(Collectors.toSet());
        Set<String> classifiers = suffixes.stream()
                .filter(f -> f.startsWith("-"))
                .map(f -> f.substring(1, f.indexOf(".")))
                .collect(Collectors.toSet());
        return new FilesInfo(extensions, classifiers);
    }

    private List<String> fetchVersionsFromDir(String groupId, String artifactId) {
        String groupPath = groupId.replace('.', '/');
        String encodedPath = URLEncoder.encode(groupPath + "/" + artifactId, StandardCharsets.UTF_8);
        HttpResponse<String> res = fetchRaw(CONTENT_URL + "/" + encodedPath);
        if (res.statusCode() >= 400) {
            return List.of();
        }

        Document doc = Jsoup.parse(res.body());
        return doc.getElementsByTag("a")
                .stream()
                .map(el -> el.attr("href"))
                .filter(href -> HTML_VERSIONS_PATTERN.matcher(href).matches())
                .map(v -> v.substring(0, v.length() - 1))
                .toList();
    }

    private List<String> fetchVersionsFromMetadata(String groupId, String artifactId) {
        String groupPath = groupId.replace('.', '/');
        String metadataPath = "%s/%s/maven-metadata.xml".formatted(groupPath, artifactId);
        String encodedPath = URLEncoder.encode(metadataPath, StandardCharsets.UTF_8);
        HttpResponse<String> res = fetchRaw(CONTENT_URL + "/" + encodedPath);
        if (res.statusCode() >= 400) {
            return List.of();
        }
        return XML_VERSIONS_PATTERN.matcher(res.body())
                .results()
                .map(m -> m.group(1))
                .toList();
    }

    private <T> T fetch(String url, ObjectMapper objectMapper, TypeReference<T> type) {
        try {
            HttpResponse<String> response = fetchRaw(url);
            if (response.statusCode() >= 400) {
                throw new BadRequestException("HTTP call failed [%s] for url [%s]".formatted(response.statusCode(), url));
            }
            return objectMapper.readValue(response.body(), type);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException("HTTP fetch deserialization failed for url [%s]".formatted(url), e);
        }
    }

    private HttpResponse<String> fetchRaw(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            return HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new UncheckedIOException("HTTP fetch failed for url [%s]".formatted(url), e);
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
        if (gav.classifier() != null && !gav.classifier().isEmpty()) {
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
