package dev.harrel.jarhell.controller;

import dev.harrel.jarhell.extension.EnvironmentTest;
import dev.harrel.jarhell.extension.Host;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.util.HttpUtil;
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
class CyclicAnalysisTest {
    private final HttpClient httpClient;
    private final Driver driver;

    @Host
    private String host;

    CyclicAnalysisTest(HttpClient httpClient, Driver driver) {
        this.httpClient = httpClient;
        this.driver = driver;
    }

    @Test
    void hardCycleWithSelf() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(host + "/api/v1/analyze"))
                .POST(HttpUtil.jsonPublisher(
                        new Gav("org.test", "cycle-self", "1.0.0")
                ))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(202);
        assertThat(response.body()).isEmpty();

        await().atMost(Duration.ofSeconds(5)).until(() -> !fetchByArtifactId("cycle-self").records().isEmpty());

        EagerResult eagerResult = fetchByArtifactId("cycle-self");
        Map<String, Object> properties = eagerResult.records().getFirst().get("n").asMap();
        assertThat(properties).containsEntry("licenses", "[{\"name\":\"MIT License\",\"url\":\"https://opensource.org/licenses/mit-license.php\"}]");
        assertJmailArtifactInfo(properties);
    }

    private EagerResult fetchByArtifactId(String id) {
        return driver.executableQuery("MATCH (n {artifactId:'%s'}) RETURN n".formatted(id)).execute();
    }

    private void assertJmailArtifactInfo(Map<String, Object> properties) {
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