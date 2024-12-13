package dev.harrel.jarhell.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import dev.harrel.jarhell.error.ErrorResponse;
import dev.harrel.jarhell.extension.EnvironmentTest;
import dev.harrel.jarhell.extension.Host;
import dev.harrel.jarhell.util.TestUtil;
import io.javalin.http.HandlerType;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static dev.harrel.jarhell.MavenApiClient.SolrArtifact;
import static org.assertj.core.api.Assertions.assertThat;

@EnvironmentTest
class MavenApiControllerTest {
    private final HttpClient httpClient;

    @Host
    private String host;

    MavenApiControllerTest(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Test
    void shouldReturnListOfVersions() throws InterruptedException, ExecutionException, TimeoutException {
        ContentResponse res = httpClient.GET(host + "/api/v1/maven/versions?groupId=dev.harrel&artifactId=json-schema");

        assertThat(res.getStatus()).isEqualTo(200);
        List<String> body = TestUtil.readJson(res.getContentAsString(), new TypeReference<>() {});
        assertThat(body).containsExactly(
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
    void shouldFailVersionsWithoutGroupId() throws InterruptedException, ExecutionException, TimeoutException {
        String uri = host + "/api/v1/maven/versions?artifactId=json-schema";
        ContentResponse res = httpClient.GET(uri);

        assertThat(res.getStatus()).isEqualTo(400);
        ErrorResponse err = TestUtil.readJson(res.getContentAsString(), ErrorResponse.class);
        assertThat(err).isEqualTo(new ErrorResponse(
                uri,
                HandlerType.GET,
                "groupId parameter is required"
        ));
    }

    @Test
    void shouldFailVersionsWithoutArtifactId() throws InterruptedException, ExecutionException, TimeoutException {
        String uri = host + "/api/v1/maven/versions?groupId=dev.harrel";
        ContentResponse res = httpClient.GET(uri);

        assertThat(res.getStatus()).isEqualTo(400);
        ErrorResponse err = TestUtil.readJson(res.getContentAsString(), ErrorResponse.class);
        assertThat(err).isEqualTo(new ErrorResponse(
                uri,
                HandlerType.GET,
                "artifactId parameter is required"
        ));
    }

    @Test
    void shouldRunSearch() throws InterruptedException, ExecutionException, TimeoutException {
        String uri = host + "/api/v1/maven/search?query=dev.harrel:json-schema";
        ContentResponse res = httpClient.GET(uri);

        assertThat(res.getStatus()).isEqualTo(200);
        List<SolrArtifact> body = TestUtil.readJson(res.getContentAsString(), new TypeReference<>() {});
        assertThat(body).hasSize(1);
        assertThat(body.getFirst()).isEqualTo(new SolrArtifact("dev.harrel", "json-schema", "1.5.0"));
    }
}