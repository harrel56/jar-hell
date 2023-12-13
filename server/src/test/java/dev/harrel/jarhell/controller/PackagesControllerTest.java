package dev.harrel.jarhell.controller;

import com.fasterxml.jackson.databind.JsonNode;
import dev.harrel.jarhell.EnvironmentTest;
import dev.harrel.jarhell.error.ErrorResponse;
import dev.harrel.jarhell.util.TestUtil;
import io.javalin.http.HandlerType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.driver.Driver;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@EnvironmentTest
class PackagesControllerTest {
    private final HttpClient httpClient;
    private final Driver driver;

    PackagesControllerTest(HttpClient httpClient, Driver driver) {
        this.httpClient = httpClient;
        this.driver = driver;
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "groupId",
            "groupId:artifactId",
            "groupId:artifactId:version:classifier:more",
            "groupId:artifactId:",
            ":something",
            ":",
            "::",
            ":::"
    })
    void shouldFailForInvalidCoordinates(String coordinate) throws IOException, InterruptedException {
        String uri = "http://localhost:8060/api/v1/packages/%s".formatted(coordinate);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .GET()
                .build();

        HttpResponse<ErrorResponse> response = httpClient.send(request, TestUtil.jsonHandler(ErrorResponse.class));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).isEqualTo(
                new ErrorResponse(uri,
                                  HandlerType.GET,
                                  "Invalid artifact coordinate format [%s]".formatted(coordinate))
        );
    }

    @Test
    void shouldReturn404ForNotFound() throws IOException, InterruptedException {
        String uri = "http://localhost:8060/api/v1/packages/org.test:lib:1.0.0";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .GET()
                .build();

        HttpResponse<ErrorResponse> response = httpClient.send(request, TestUtil.jsonHandler(ErrorResponse.class));

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).isEqualTo(
                new ErrorResponse(uri,
                                  HandlerType.GET,
                                  "Package with coordinates [org.test:lib:1.0.0] not found")
        );
    }

    @Test
    void shouldFindByGav() throws IOException, InterruptedException {
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(tx ->
                tx.run("CREATE (:Artifact {groupId: 'org.test', artifactId: 'lib', version: '1.0.0'})")
            );
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8060/api/v1/packages/org.test:lib:1.0.0"))
                .GET()
                .build();

        HttpResponse<JsonNode> response = httpClient.send(request, TestUtil.jsonHandler(JsonNode.class));

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = response.body();
        assertThat(body.get("groupId").asText()).isEqualTo("org.test");
        assertThat(body.get("artifactId").asText()).isEqualTo("lib");
        assertThat(body.get("version").asText()).isEqualTo("1.0.0");
        assertThat(body.get("licenses").elements()).toIterable().isEmpty();
        assertThat(body.get("dependencies").elements()).toIterable().isEmpty();
    }

    @Test
    void shouldFindByGavWithClassifier() throws IOException, InterruptedException {
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(tx ->
                tx.run("CREATE (:Artifact {groupId: 'org.test', artifactId: 'lib', version: '1.0.0', classifier: 'doc'})")
            );
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8060/api/v1/packages/org.test:lib:1.0.0:doc"))
                .GET()
                .build();

        HttpResponse<JsonNode> response = httpClient.send(request, TestUtil.jsonHandler(JsonNode.class));

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = response.body();
        assertThat(body.get("groupId").asText()).isEqualTo("org.test");
        assertThat(body.get("artifactId").asText()).isEqualTo("lib");
        assertThat(body.get("version").asText()).isEqualTo("1.0.0");
        assertThat(body.get("classifier").asText()).isEqualTo("doc");
        assertThat(body.get("licenses").elements()).toIterable().isEmpty();
        assertThat(body.get("dependencies").elements()).toIterable().isEmpty();
    }

    @Test
    void shouldFindByGavDoesNotReturnWithClassifier() throws IOException, InterruptedException {
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(tx ->
                tx.run("CREATE (:Artifact {groupId: 'org.test', artifactId: 'lib', version: '1.0.0', classifier: 'doc'})")
            );
        }

        String uri = "http://localhost:8060/api/v1/packages/org.test:lib:1.0.0";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .GET()
                .build();

        HttpResponse<ErrorResponse> response = httpClient.send(request, TestUtil.jsonHandler(ErrorResponse.class));

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).isEqualTo(
                new ErrorResponse(uri,
                                  HandlerType.GET,
                                  "Package with coordinates [org.test:lib:1.0.0] not found")
        );
    }
}