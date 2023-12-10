package dev.harrel.jarhell.controller;

import dev.harrel.jarhell.EnvironmentTest;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.util.TestUtil;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@EnvironmentTest
class AnalyzeControllerTest {
    private final Driver driver;

    AnalyzeControllerTest(Driver driver) {
        this.driver = driver;
    }

    @Test
    void name() throws IOException, InterruptedException {
        HttpClient httpClient = HttpClient.newHttpClient();
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
        // todo: add assertions
        System.out.println(eagerResult);
    }

    private EagerResult fetchAllNodes() {
        return driver.executableQuery("MATCH (n) RETURN n").execute();
    }
}