package dev.harrel.jarhell;

import dev.harrel.jarhell.model.ArtifactInfo;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.model.JarInfo;
import dev.harrel.jarhell.model.PomInfo;
import io.nats.jparse.Json;
import io.nats.jparse.Path;
import io.nats.jparse.node.RootNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Analyzer {
    private final HttpClient httpClient;
    private final JarAnalyzer jarAnalyzer;
    private final PomAnalyzer pomAnalyzer;

    Analyzer() {
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
        JarInfo jarInfo = jarAnalyzer.analyzeJar(gav);
        PomInfo pomInfo = pomAnalyzer.analyzePom(gav);

        return ArtifactInfo.create(gav, jarInfo, pomInfo);
    }

    private String fetchLatestVersion(String group, String artifact) throws IOException, InterruptedException {
        String query = "?q=g:%s+AND+a:%s".formatted(group, artifact);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://search.maven.org/solrsearch/select" + query))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        RootNode rootNode = Json.toRootNode(response.body());
        return Path.atPath("response.docs[0].latestVersion", rootNode).toJsonString();
    }
}

