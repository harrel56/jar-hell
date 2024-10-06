package dev.harrel.jarhell.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import dev.harrel.jarhell.error.ErrorResponse;
import dev.harrel.jarhell.extension.EnvironmentTest;
import dev.harrel.jarhell.extension.Host;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.util.HttpUtil;
import io.javalin.http.HandlerType;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.EagerResult;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@EnvironmentTest
class AnalyzeControllerTest {
    private final HttpClient httpClient;
    private final Driver driver;

    @Host
    private String host;

    AnalyzeControllerTest(HttpClient httpClient, Driver driver) {
        this.httpClient = httpClient;
        this.driver = driver;
    }

    @Test
    void shouldAnalyzeStandaloneLib() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(host + "/api/v1/analyze"))
                .POST(HttpUtil.jsonPublisher(
                        new Gav("com.sanctionco.jmail", "jmail", "1.6.2")
                ))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(202);
        assertThat(response.body()).isEmpty();

        await().atMost(Duration.ofSeconds(5)).until(() -> !fetchAllNodes().records().isEmpty());

        EagerResult eagerResult = fetchAllNodes();
        Map<String, Object> properties = eagerResult.records().getFirst().get("n").asMap();
        assertThat(properties).containsEntry("licenses", "[{\"name\":\"MIT License\",\"url\":\"https://opensource.org/licenses/mit-license.php\"}]");
        assertArtifactInfo(properties);
    }

    @Test
    void shouldAnalyzeAndWaitForStandaloneLib() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(host + "/api/v1/analyze-and-wait"))
                .POST(HttpUtil.jsonPublisher(
                        new Gav("com.sanctionco.jmail", "jmail", "1.6.2")
                ))
                .build();

        HttpResponse<Map<String, Object>> response = httpClient.send(request, HttpUtil.jsonHandler(new TypeReference<>() {}));
        assertThat(response.statusCode()).isEqualTo(200);
        Map<String, Object> properties = response.body();
        assertThat(properties).isNotNull();
        assertThat(properties).containsEntry("licenses", List.of(Map.of(
                "name", "MIT License",
                "url", "https://opensource.org/licenses/mit-license.php"
        )));
        assertThat(properties).containsEntry("dependencies", List.of());
        assertArtifactInfo(properties);
    }

    @Test
    void shouldReturnNotFound() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(host + "/api/v1/analyze"))
                .POST(HttpUtil.jsonPublisher(
                        new Gav("org.test", "non-existent", "9.9.9")
                ))
                .build();

        HttpResponse<ErrorResponse> response = httpClient.send(request, HttpUtil.jsonHandler(ErrorResponse.class));
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).isEqualTo(
                new ErrorResponse(host + "/api/v1/analyze",
                        HandlerType.POST,
                        "Package with coordinates [org.test:non-existent:9.9.9] not found")
        );
    }

    private EagerResult fetchAllNodes() {
        return driver.executableQuery("MATCH (n) RETURN n").execute();
    }

    private void assertArtifactInfo(Map<String, Object> properties) {
        assertThat(properties).contains(
                Map.entry("groupId", "com.sanctionco.jmail"),
                Map.entry("artifactId", "jmail"),
                Map.entry("name", "jmail"),
                Map.entry("description", "A modern, fast, zero-dependency library for working with emails in Java"),
                Map.entry("packaging", "jar"),
                Map.entry("packageSize", 30629L),
                Map.entry("version", "1.6.2"),
                Map.entry("url", "https://github.com/RohanNagar/jmail"),
                Map.entry("bytecodeVersion", "52.0"),
                Map.entry("totalSize", 30629L),
                Map.entry("classifiers", List.of("javadoc", "sources"))
        );
    }
}