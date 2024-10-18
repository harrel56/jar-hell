package dev.harrel.jarhell.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import dev.harrel.jarhell.error.ErrorResponse;
import dev.harrel.jarhell.extension.EnvironmentTest;
import dev.harrel.jarhell.extension.Host;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.util.HttpUtil;
import io.javalin.http.HandlerType;
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
class CyclicAnalysisTest {
    private final HttpClient httpClient;

    @Host
    private String host;

    CyclicAnalysisTest(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Test
    void hardCycleWithSelfFails() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(host + "/api/v1/analyze-and-wait"))
                .POST(HttpUtil.jsonPublisher(
                        new Gav("org.test", "cycle-self", "1.0.0")
                ))
                .build();

        HttpResponse<ErrorResponse> response = httpClient.send(request, HttpUtil.jsonHandler(ErrorResponse.class));
        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.body()).isEqualTo(
                new ErrorResponse(host + "/api/v1/analyze-and-wait",
                        HandlerType.POST,
                        "HARD CYCLE found, preceding: [], cycle: [org.test:cycle-self:1.0.0, org.test:cycle-self:1.0.0]")
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void softCycleWithSelfPasses() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(host + "/api/v1/analyze-and-wait"))
                .POST(HttpUtil.jsonPublisher(
                        new Gav("org.test", "cycle-self-soft", "1.0.0")
                ))
                .build();

        HttpResponse<Map<String, Object>> response = httpClient.send(request, HttpUtil.jsonHandler(new TypeReference<>() {}));
        assertThat(response.statusCode()).isEqualTo(200);
        Map<String, Object> properties = response.body();
        assertThat(properties).containsEntry("artifactId", "cycle-self-soft");
        List<Map<String, Object>> deps = (List<Map<String, Object>>) properties.get("dependencies");
        assertThat(deps).hasSize(1);
        assertThat(deps.getFirst())
                .containsEntry("optional", true)
                .containsEntry("scope", "compile");
        Map<String, Object> depProps = (Map<String, Object>) deps.getFirst().get("artifact");
        assertThat(depProps)
                .containsEntry("artifactId", "cycle-self-soft")
                .doesNotContainKey("dependencies");
    }

    @Test
    void hardCycleWithThreeArtifactsFails() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(host + "/api/v1/analyze-and-wait"))
                .POST(HttpUtil.jsonPublisher(
                        new Gav("org.test", "cycle1", "1.0.0")
                ))
                .build();

        HttpResponse<ErrorResponse> response = httpClient.send(request, HttpUtil.jsonHandler(ErrorResponse.class));
        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.body()).isEqualTo(
                new ErrorResponse(host + "/api/v1/analyze-and-wait",
                        HandlerType.POST,
                        "HARD CYCLE found, preceding: [], cycle: [org.test:cycle1:1.0.0, org.test:cycle2:1.0.0, org.test:cycle3:1.0.0, org.test:cycle1:1.0.0]")
        );
    }

    @Test
    void hardCycleWithFourArtifactsFails() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(host + "/api/v1/analyze-and-wait"))
                .POST(HttpUtil.jsonPublisher(
                        new Gav("org.test", "pre-cycle", "1.0.0")
                ))
                .build();

        HttpResponse<ErrorResponse> response = httpClient.send(request, HttpUtil.jsonHandler(ErrorResponse.class));
        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.body()).isEqualTo(
                new ErrorResponse(host + "/api/v1/analyze-and-wait",
                        HandlerType.POST,
                        "HARD CYCLE found, preceding: [org.test:pre-cycle:1.0.0], cycle: [org.test:cycle3:1.0.0, org.test:cycle1:1.0.0, org.test:cycle2:1.0.0, org.test:cycle3:1.0.0]")
        );
    }

//    @Test
//    @SuppressWarnings("unchecked")
//    void softCycleWithThreeArtifactsPasses() throws IOException, InterruptedException {
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create(host + "/api/v1/analyze-and-wait"))
//                .POST(HttpUtil.jsonPublisher(
//                        new Gav("org.test", "cycle1-soft", "1.0.0")
//                ))
//                .build();
//
//        HttpResponse<Map<String, Object>> response = httpClient.send(request, HttpUtil.jsonHandler(new TypeReference<>() {}));
//        assertThat(response.statusCode()).isEqualTo(200);
//        Map<String, Object> properties = response.body();
//        assertThat(properties).containsEntry("artifactId", "cycle1-soft");
//        List<Map<String, Object>> deps = (List<Map<String, Object>>) properties.get("dependencies");
//        assertThat(deps).hasSize(2);
//        assertThat(deps.get(0))
//                .containsEntry("optional", true)
//                .containsEntry("scope", "compile");
//        assertThat(deps.get(1))
//                .containsEntry("optional", true)
//                .containsEntry("scope", "compile");
//        Map<String, Object> depProps1 = (Map<String, Object>) deps.get(0).get("artifact");
//        Map<String, Object> depProps2 = (Map<String, Object>) deps.get(1).get("artifact");
//    }
}