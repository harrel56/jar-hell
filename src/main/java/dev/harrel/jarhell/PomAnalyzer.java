package dev.harrel.jarhell;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.model.PomInfo;
import dev.harrel.jarhell.model.pom.ProjectModel;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class PomAnalyzer {
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public PomAnalyzer(HttpClient httpClient) {
        this.httpClient = httpClient;
        this.mapper = new XmlMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    PomInfo analyzePom(Gav gav) throws IOException, InterruptedException {
        ProjectModel projectModel = fetchProjectModel(gav);
        return new PomInfo(projectModel.packaging(), projectModel.dependencies());
    }

    private ProjectModel fetchProjectModel(Gav gav) throws IOException, InterruptedException {
        String groupPath = gav.groupId().replace('.', '/');
        String fileName = "%s-%s.pom".formatted(gav.artifactId(), gav.version());
        String query = "?filepath=%s/%s/%s/%s".formatted(groupPath, gav.artifactId(), gav.version(), fileName);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://search.maven.org/remotecontent" + query))
                .GET()
                .build();
        String response = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();

        return mapper.readValue(response, ProjectModel.class);
    }
}

