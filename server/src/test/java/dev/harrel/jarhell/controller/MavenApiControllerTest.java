package dev.harrel.jarhell.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import dev.harrel.jarhell.MavenApiClient;
import dev.harrel.jarhell.error.ErrorResponse;
import dev.harrel.jarhell.extension.EnvironmentTest;
import dev.harrel.jarhell.util.HttpUtil;
import io.javalin.http.HandlerType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static dev.harrel.jarhell.MavenApiClient.*;
import static org.assertj.core.api.Assertions.assertThat;

@EnvironmentTest
class MavenApiControllerTest {

    private final HttpClient httpClient;

    MavenApiControllerTest(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Test
    void shouldReturnListOfVersions() throws IOException, InterruptedException {
        String uri = "http://localhost:8060/api/v1/maven/versions?groupId=dev.harrel&artifactId=json-schema";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .GET()
                .build();

        HttpResponse<List<String>> response = httpClient.send(request, HttpUtil.jsonHandler(new TypeReference<>() {}));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).containsExactly(
                "1.0.0",
                "1.1.0",
                "1.2.0",
                "1.2.1",
                "1.2.2",
                "1.3.0",
                "1.3.1",
                "1.3.2",
                "1.3.3",
                "1.3.4",
                "1.4.0",
                "1.4.1",
                "1.4.2",
                "1.4.3",
                "1.5.0"
                );
    }

    @Test
    void shouldFailVersionsWithoutGroupId() throws IOException, InterruptedException {
        String uri = "http://localhost:8060/api/v1/maven/versions?artifactId=json-schema";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .GET()
                .build();

        HttpResponse<ErrorResponse> response = httpClient.send(request, HttpUtil.jsonHandler(ErrorResponse.class));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).isEqualTo(new ErrorResponse(
                uri,
                HandlerType.GET,
                "groupId parameter is required"
        ));
    }

    @Test
    void shouldFailVersionsWithoutArtifactId() throws IOException, InterruptedException {
        String uri = "http://localhost:8060/api/v1/maven/versions?groupId=dev.harrel";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .GET()
                .build();

        HttpResponse<ErrorResponse> response = httpClient.send(request, HttpUtil.jsonHandler(ErrorResponse.class));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).isEqualTo(new ErrorResponse(
                uri,
                HandlerType.GET,
                "artifactId parameter is required"
        ));
    }

    @Test
    void shouldRunSearch() throws IOException, InterruptedException {
        String uri = "http://localhost:8060/api/v1/maven/search?query=dev.harrel:json-schema";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .GET()
                .build();

        HttpResponse<List<SolrArtifact>> response = httpClient.send(request, HttpUtil.jsonHandler(new TypeReference<>() {}));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).hasSize(1);
        assertThat(response.body().getFirst()).isEqualTo(new SolrArtifact("dev.harrel", "json-schema"));
    }
}