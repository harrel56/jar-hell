package dev.harrel.jarhell.controller;

import dev.harrel.jarhell.EnvironmentTest;
import dev.harrel.jarhell.error.ErrorResponse;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.util.TestUtil;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
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

    AnalyzeControllerTest(HttpClient httpClient, Driver driver) {
        this.httpClient = httpClient;
        this.driver = driver;
    }

    @Test
    void shouldAnalyzeStandaloneLib() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8060/api/v1/analyze"))
                .POST(TestUtil.jsonPublisher(
                        new Gav("com.sanctionco.jmail", "jmail", "1.6.2")
                ))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(202);
        assertThat(response.body()).isEmpty();

        await().atMost(Duration.ofSeconds(5)).until(() -> !fetchAllNodes().records().isEmpty());

        EagerResult eagerResult = fetchAllNodes();
        Map<String, Object> properties = eagerResult.records().getFirst().get("n").asMap();
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
                Map.entry("licenses", "[{\"name\":\"MIT License\",\"url\":\"https://opensource.org/licenses/mit-license.php\"}]"),
                Map.entry("totalSize", 30629L),
                Map.entry("bytecodeVersion", "52.0"),
                Map.entry("classifiers", List.of("javadoc", "sources"))
        );
    }

    @Test
    void shouldReturnNotFound() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8060/api/v1/analyze"))
                .POST(TestUtil.jsonPublisher(
                        new Gav("org.test", "non-existent", "9.9.9")
                ))
                .build();

        HttpResponse<ErrorResponse> response = httpClient.send(request, TestUtil.jsonHandler(ErrorResponse.class));
        assertThat(response.statusCode()).isEqualTo(404);
        ErrorResponse err = response.body();
        assertThat(err.url()).isEqualTo("http://localhost:8060/api/v1/analyze");
        assertThat(err.method()).isEqualTo(HandlerType.POST);
        assertThat(err.message()).isEqualTo("Package with coordinates [org.test:non-existent:9.9.9] not found");
    }

    private EagerResult fetchAllNodes() {
        return driver.executableQuery("MATCH (n) RETURN n").execute();
    }
}