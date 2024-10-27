package dev.harrel.jarhell.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import dev.harrel.jarhell.extension.EnvironmentTest;
import dev.harrel.jarhell.extension.Host;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.util.HttpUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@EnvironmentTest
@SuppressWarnings("unchecked")
class CyclicAnalysisTest {
    private final HttpClient httpClient;

    @Host
    private String host;

    CyclicAnalysisTest(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Test
    void hardCycleWithSelfIsIgnored() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(host + "/api/v1/analyze-and-wait"))
                .POST(HttpUtil.jsonPublisher(
                        new Gav("org.test", "cycle-self", "1.0.0")
                ))
                .build();

        HttpResponse<Map<String, Object>> response = httpClient.send(request, HttpUtil.jsonHandler(new TypeReference<>() {}));
        assertThat(response.statusCode()).isEqualTo(200);
        Map<String, Object> properties = response.body();
        assertThat(properties)
                .containsEntry("artifactId", "cycle-self")
                .containsEntry("packageSize", 2105L)
                .containsEntry("effectiveValues", Map.of("size", 2105L));
        List<Map<String, Object>> deps = (List<Map<String, Object>>) properties.get("dependencies");
        assertThat(deps).isEmpty();
    }

    @Test
    void softCycleWithSelfIsIgnored() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(host + "/api/v1/analyze-and-wait"))
                .POST(HttpUtil.jsonPublisher(
                        new Gav("org.test", "cycle-self-soft", "1.0.0")
                ))
                .build();

        HttpResponse<Map<String, Object>> response = httpClient.send(request, HttpUtil.jsonHandler(new TypeReference<>() {}));
        assertThat(response.statusCode()).isEqualTo(200);
        Map<String, Object> properties = response.body();
        assertThat(properties).containsEntry("artifactId", "cycle-self-soft")
                .containsEntry("packageSize", 2105L)
                .containsEntry("effectiveValues", Map.of("size", 2105L));
        List<Map<String, Object>> deps = (List<Map<String, Object>>) properties.get("dependencies");
        assertThat(deps).isEmpty();
    }

    @Test
    void hardCycleWithThreeArtifactsIsComputedCorrectly() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(host + "/api/v1/analyze-and-wait"))
                .POST(HttpUtil.jsonPublisher(
                        new Gav("org.test", "cycle1", "1.0.0")
                ))
                .build();

        HttpResponse<Map<String, Object>> response = httpClient.send(request, HttpUtil.jsonHandler(new TypeReference<>() {}));
        assertThat(response.statusCode()).isEqualTo(200);
        Map<String, Object> properties = response.body();
        assertThat(properties)
                .containsEntry("artifactId", "cycle1")
                .containsEntry("packageSize", 2105L)
                .containsEntry("effectiveValues", Map.of("size", 6315L));
        List<Map<String, Object>> deps = (List<Map<String, Object>>) properties.get("dependencies");
        assertThat(deps).hasSize(1);
        assertThat(deps.getFirst())
                .containsEntry("optional", false)
                .containsEntry("scope", "compile");
        assertThat((Map<String, Object>) deps.getFirst().get("artifact"))
                .containsEntry("artifactId", "cycle2")
                .containsEntry("packageSize", 2105L)
                .containsEntry("effectiveValues", Map.of("size", 6315L));
    }

    @Test
    void hardCycleWithFourArtifactsIsComputedCorrectly() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(host + "/api/v1/analyze-and-wait"))
                .POST(HttpUtil.jsonPublisher(
                        new Gav("org.test", "pre-cycle", "1.0.0")
                ))
                .build();

        HttpResponse<Map<String, Object>> response = httpClient.send(request, HttpUtil.jsonHandler(new TypeReference<>() {}));
        assertThat(response.statusCode()).isEqualTo(200);
        Map<String, Object> properties = response.body();
        assertThat(properties)
                .containsEntry("artifactId", "pre-cycle")
                .containsEntry("packageSize", 2105L)
                .containsEntry("effectiveValues", Map.of("size", 2105L));// all deps are optional
        List<Map<String, Object>> deps = (List<Map<String, Object>>) properties.get("dependencies");
        assertThat(deps).hasSize(1);
        assertThat(deps.getFirst())
                .containsEntry("optional", true)
                .containsEntry("scope", "compile");
        assertThat((Map<String, Object>) deps.getFirst().get("artifact"))
                .containsEntry("artifactId", "cycle3")
                .containsEntry("packageSize", 2105L)
                .containsEntry("effectiveValues", Map.of("size", 6315L));
    }
}