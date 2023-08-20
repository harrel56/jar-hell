package dev.harrel.jarhell;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.harrel.jarhell.model.ArtifactInfo;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.model.PackageInfo;
import dev.harrel.jarhell.model.PomInfo;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.Set;

public class Analyzer {
    private final ObjectMapper objectMapper;
    private final ApiClient apiClient;
    private final PackageAnalyzer packageAnalyzer;

    public Analyzer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        this.apiClient = new ApiClient(objectMapper, httpClient);
        this.packageAnalyzer = new PackageAnalyzer(httpClient);
    }

    public ArtifactInfo analyze(String groupId, String artifactId) throws IOException, InterruptedException {
        String version = apiClient.fetchLatestVersion(groupId, artifactId);
        return analyze(new Gav(groupId, artifactId, version));
    }

    public ArtifactInfo analyze(Gav gav) throws IOException, InterruptedException {
        Set<String> files = apiClient.fetchAvailableFiles(gav);
        if (!files.contains("pom")) {
            throw new IllegalArgumentException("Artifact is missing pom file: " + gav);
        }
        PomInfo pomInfo = apiClient.fetchPomInfo(gav);
        PackageInfo packageInfo = packageAnalyzer.analyzeJar(gav, files, pomInfo.packaging());

        return ArtifactInfo.create(gav, packageInfo, pomInfo);
    }
}

