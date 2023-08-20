package dev.harrel.jarhell;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.harrel.jarhell.model.ArtifactInfo;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.model.JarInfo;
import dev.harrel.jarhell.model.PomInfo;
import dev.harrel.jarhell.model.central.ArtifactDoc;
import dev.harrel.jarhell.model.central.SelectResponse;
import dev.harrel.jarhell.model.central.VersionDoc;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;
import java.util.stream.Collectors;

public class Analyzer {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final JarAnalyzer jarAnalyzer;
    private final PomAnalyzer pomAnalyzer;

    public Analyzer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        this.pomAnalyzer = new PomAnalyzer(httpClient);
        this.jarAnalyzer = new JarAnalyzer(httpClient);
    }

    public ArtifactInfo analyze(String groupId, String artifactId) throws IOException, InterruptedException {
        String version = fetchLatestVersion(groupId, artifactId);
        return analyze(new Gav(groupId, artifactId, version));
    }

    public ArtifactInfo analyze(Gav gav) throws IOException, InterruptedException {
        Set<String> files = fetchAvailableFiles(gav);
        if (!files.contains(".pom")) {
            throw new IllegalArgumentException("Artifact is missing pom file: " + gav);
        }
        System.out.println(files);
        PomInfo pomInfo = pomAnalyzer.analyzePom(gav);
        JarInfo jarInfo = jarAnalyzer.analyzeJar(gav);

        return ArtifactInfo.create(gav, jarInfo, pomInfo);
    }

    private String fetchLatestVersion(String groupId, String artifactId) throws IOException, InterruptedException {
        String query = "?q=g:%s+AND+a:%s".formatted(groupId, artifactId);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://search.maven.org/solrsearch/select" + query))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        SelectResponse<ArtifactDoc> selectResponse = objectMapper.readValue(response.body(), new TypeReference<>() {});
        if (selectResponse.response().numFound() < 1) {
            throw new IllegalArgumentException("Artifact couldn't be found: %s:%s".formatted(groupId, artifactId));
        }
        return selectResponse.response().docs().get(0).latestVersion();
    }

    private Set<String> fetchAvailableFiles(Gav gav) throws IOException, InterruptedException {
        String query = "?q=g:%s+AND+a:%s+AND+v:%s".formatted(gav.groupId(), gav.artifactId(), gav.version());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://search.maven.org/solrsearch/select" + query))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        SelectResponse<VersionDoc> selectResponse = objectMapper.readValue(response.body(), new TypeReference<>() {});
        if (selectResponse.response().numFound() < 1) {
            throw new IllegalArgumentException("Artifact couldn't be found: " + gav);
        }
        return selectResponse.response().docs().get(0).ec().stream()
                .filter(f -> !f.startsWith("."))
                .filter(f -> !f.endsWith("sha256") && !f.endsWith("sha512"))
                .collect(Collectors.toSet());
    }
}

