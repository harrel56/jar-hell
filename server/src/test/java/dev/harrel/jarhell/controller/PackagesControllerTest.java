package dev.harrel.jarhell.controller;

import com.fasterxml.jackson.databind.JsonNode;
import dev.harrel.jarhell.EnvironmentTest;
import dev.harrel.jarhell.error.ErrorResponse;
import dev.harrel.jarhell.util.HttpUtil;
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
import java.util.List;

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

        HttpResponse<ErrorResponse> response = httpClient.send(request, HttpUtil.jsonHandler(ErrorResponse.class));

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

        HttpResponse<ErrorResponse> response = httpClient.send(request, HttpUtil.jsonHandler(ErrorResponse.class));

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
            session.executeWriteWithoutResult(
                    tx -> tx.run("CREATE (:Artifact {groupId: 'org.test', artifactId: 'lib', version: '1.0.0'})")
            );
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8060/api/v1/packages/org.test:lib:1.0.0"))
                .GET()
                .build();

        HttpResponse<JsonNode> response = httpClient.send(request, HttpUtil.jsonHandler(JsonNode.class));

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = response.body();
        assertArtifact(body, "org.test", "lib", "1.0.0", null);
        assertThat(body.get("dependencies").elements()).toIterable().isEmpty();
    }

    @Test
    void shouldFindByGavWithClassifier() throws IOException, InterruptedException {
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(
                    tx -> tx.run("CREATE (:Artifact {groupId: 'org.test', artifactId: 'lib', version: '1.0.0', classifier: 'doc'})")
            );
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8060/api/v1/packages/org.test:lib:1.0.0:doc"))
                .GET()
                .build();

        HttpResponse<JsonNode> response = httpClient.send(request, HttpUtil.jsonHandler(JsonNode.class));

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = response.body();
        assertArtifact(body, "org.test", "lib", "1.0.0", "doc");
        assertThat(body.get("dependencies").elements()).toIterable().isEmpty();
    }

    @Test
    void shouldFindByGavDoesNotReturnWithClassifier() throws IOException, InterruptedException {
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(
                    tx -> tx.run("CREATE (:Artifact {groupId: 'org.test', artifactId: 'lib', version: '1.0.0', classifier: 'doc'})")
            );
        }

        String uri = "http://localhost:8060/api/v1/packages/org.test:lib:1.0.0";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .GET()
                .build();

        HttpResponse<ErrorResponse> response = httpClient.send(request, HttpUtil.jsonHandler(ErrorResponse.class));

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).isEqualTo(
                new ErrorResponse(uri,
                        HandlerType.GET,
                        "Package with coordinates [org.test:lib:1.0.0] not found")
        );
    }

    @Test
    void shouldFindWithSingleDirectDependency() throws IOException, InterruptedException {
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(
                    tx -> tx.run("""
                            CREATE (:Artifact {groupId: 'org.test', artifactId: 'lib', version: '1.0.0'})\
                            -[:DEPENDS_ON {optional: false, scope: 'runtime'}]->\
                            (:Artifact {groupId: 'org.test', artifactId: 'dep1', version: '1.0.0'})
                            """)
            );
        }

        String uri = "http://localhost:8060/api/v1/packages/org.test:lib:1.0.0";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .GET()
                .build();

        HttpResponse<JsonNode> response = httpClient.send(request, HttpUtil.jsonHandler(JsonNode.class));

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = response.body();
        assertArtifact(body, "org.test", "lib", "1.0.0", null);

        List<JsonNode> dependencies = TestUtil.iteratorToList(body.get("dependencies").elements());
        assertThat(dependencies).hasSize(1);
        assertThat(dependencies.getFirst().get("optional").asBoolean()).isFalse();
        assertThat(dependencies.getFirst().get("scope").asText()).isEqualTo("runtime");

        JsonNode dep1 = dependencies.getFirst().get("artifact");
        assertArtifact(dep1, "org.test", "dep1", "1.0.0", null);
        assertThat(dep1.get("dependencies").elements()).toIterable().isEmpty();
    }

    @Test
    void shouldFindWithMultipleDirectDependencies() throws IOException, InterruptedException {
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(
                    tx -> tx.run("""
                            CREATE (root:Artifact {groupId: 'org.test', artifactId: 'lib', version: '1.0.0'})\
                            -[:DEPENDS_ON {optional: true, scope: 'runtime'}]->\
                            (:Artifact {groupId: 'org.test', artifactId: 'dep1', version: '1.0.0'}),
                                                        
                            (root)-[:DEPENDS_ON {optional: false, scope: 'compile'}]->\
                            (:Artifact {groupId: 'org.test', artifactId: 'dep2', version: '1.0.0'})
                            """)
            );
        }

        String uri = "http://localhost:8060/api/v1/packages/org.test:lib:1.0.0";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .GET()
                .build();

        HttpResponse<JsonNode> response = httpClient.send(request, HttpUtil.jsonHandler(JsonNode.class));

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = response.body();
        assertArtifact(body, "org.test", "lib", "1.0.0", null);

        List<JsonNode> dependencies = TestUtil.iteratorToList(body.get("dependencies").elements());
        assertThat(dependencies).hasSize(2);

        assertThat(dependencies.get(0).get("optional").asBoolean()).isTrue();
        assertThat(dependencies.get(0).get("scope").asText()).isEqualTo("runtime");
        JsonNode dep1 = dependencies.get(0).get("artifact");
        assertArtifact(dep1, "org.test", "dep1", "1.0.0", null);
        assertThat(dep1.get("dependencies").elements()).toIterable().isEmpty();

        assertThat(dependencies.get(1).get("optional").asBoolean()).isFalse();
        assertThat(dependencies.get(1).get("scope").asText()).isEqualTo("compile");
        JsonNode dep2 = dependencies.get(1).get("artifact");
        assertArtifact(dep2, "org.test", "dep2", "1.0.0", null);
        assertThat(dep2.get("dependencies").elements()).toIterable().isEmpty();
    }

    @Test
    void shouldFindWithTransitiveDependencies() throws IOException, InterruptedException {
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(
                    tx -> tx.run("""
                            CREATE (:Artifact {groupId: 'org.test', artifactId: 'lib', version: '1.0.0'})\
                            -[:DEPENDS_ON {optional: false, scope: 'runtime'}]->\
                            (:Artifact {groupId: 'org.test', artifactId: 'dep1', version: '1.0.0'})
                            -[:DEPENDS_ON {optional: false, scope: 'runtime'}]->\
                            (:Artifact {groupId: 'org.test', artifactId: 'dep2', version: '1.0.0'})
                            """)
            );
        }

        String uri = "http://localhost:8060/api/v1/packages/org.test:lib:1.0.0";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .GET()
                .build();

        HttpResponse<JsonNode> response = httpClient.send(request, HttpUtil.jsonHandler(JsonNode.class));

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = response.body();
        assertArtifact(body, "org.test", "lib", "1.0.0", null);

        List<JsonNode> dependencies = TestUtil.iteratorToList(body.get("dependencies").elements());
        assertThat(dependencies).hasSize(1);
        assertThat(dependencies.getFirst().get("optional").asBoolean()).isFalse();
        assertThat(dependencies.getFirst().get("scope").asText()).isEqualTo("runtime");

        JsonNode dep1 = dependencies.getFirst().get("artifact");
        assertArtifact(dep1, "org.test", "dep1", "1.0.0", null);
        List<JsonNode> transitiveDeps = TestUtil.iteratorToList(dep1.get("dependencies").elements());
        assertThat(transitiveDeps).hasSize(1);
        assertThat(transitiveDeps.getFirst().get("optional").asBoolean()).isFalse();
        assertThat(transitiveDeps.getFirst().get("scope").asText()).isEqualTo("runtime");

        JsonNode dep2 = transitiveDeps.getFirst().get("artifact");
        assertArtifact(dep2, "org.test", "dep2", "1.0.0", null);
        assertThat(dep2.get("dependencies").elements()).toIterable().isEmpty();
    }

    @Test
    void shouldFindWithDepth0() throws IOException, InterruptedException {
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(
                    tx -> tx.run("""
                            CREATE (:Artifact {groupId: 'org.test', artifactId: 'lib', version: '1.0.0'})\
                            -[:DEPENDS_ON {optional: false, scope: 'runtime'}]->\
                            (:Artifact {groupId: 'org.test', artifactId: 'dep1', version: '1.0.0'})
                            -[:DEPENDS_ON {optional: false, scope: 'runtime'}]->\
                            (:Artifact {groupId: 'org.test', artifactId: 'dep2', version: '1.0.0'})
                            """)
            );
        }

        String uri = "http://localhost:8060/api/v1/packages/org.test:lib:1.0.0?depth=0";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .GET()
                .build();

        HttpResponse<JsonNode> response = httpClient.send(request, HttpUtil.jsonHandler(JsonNode.class));

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = response.body();
        assertArtifact(body, "org.test", "lib", "1.0.0", null);
        assertThat(body.get("dependencies").elements()).toIterable().isEmpty();
    }

    @Test
    void shouldFindWithDepth1() throws IOException, InterruptedException {
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(
                    tx -> tx.run("""
                            CREATE (:Artifact {groupId: 'org.test', artifactId: 'lib', version: '1.0.0'})\
                            -[:DEPENDS_ON {optional: false, scope: 'runtime'}]->\
                            (:Artifact {groupId: 'org.test', artifactId: 'dep1', version: '1.0.0'})
                            -[:DEPENDS_ON {optional: false, scope: 'runtime'}]->\
                            (:Artifact {groupId: 'org.test', artifactId: 'dep2', version: '1.0.0'})
                            """)
            );
        }

        String uri = "http://localhost:8060/api/v1/packages/org.test:lib:1.0.0?depth=1";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .GET()
                .build();

        HttpResponse<JsonNode> response = httpClient.send(request, HttpUtil.jsonHandler(JsonNode.class));

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = response.body();
        assertArtifact(body, "org.test", "lib", "1.0.0", null);

        List<JsonNode> dependencies = TestUtil.iteratorToList(body.get("dependencies").elements());
        assertThat(dependencies).hasSize(1);
        assertThat(dependencies.getFirst().get("optional").asBoolean()).isFalse();
        assertThat(dependencies.getFirst().get("scope").asText()).isEqualTo("runtime");

        JsonNode dep1 = dependencies.getFirst().get("artifact");
        assertArtifact(dep1, "org.test", "dep1", "1.0.0", null);
    }

    @Test
    void shouldReturn400ForInvalidDepth() throws IOException, InterruptedException {
        String uri = "http://localhost:8060/api/v1/packages/org.test:lib:1.0.0?depth=hello";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .GET()
                .build();

        HttpResponse<ErrorResponse> response = httpClient.send(request, HttpUtil.jsonHandler(ErrorResponse.class));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).isEqualTo(
                new ErrorResponse(uri,
                        HandlerType.GET,
                        "java.lang.NumberFormatException: For input string: \"hello\"")
        );
    }

    static void assertArtifact(JsonNode node,
                                   String groupId,
                                   String artifactId,
                                   String version,
                                   String classifier) {
        assertThat(node.get("groupId").asText()).isEqualTo(groupId);
        assertThat(node.get("artifactId").asText()).isEqualTo(artifactId);
        assertThat(node.get("version").asText()).isEqualTo(version);
        if (classifier != null) {
            assertThat(node.get("classifier").asText()).isEqualTo(classifier);
        }
        assertThat(node.get("licenses").elements()).toIterable().isEmpty();
    }
}