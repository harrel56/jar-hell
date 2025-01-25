package dev.harrel.jarhell.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dev.harrel.jarhell.error.ErrorResponse;
import dev.harrel.jarhell.extension.EnvironmentTest;
import dev.harrel.jarhell.extension.Host;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.util.TestUtil;
import io.javalin.http.HandlerType;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.driver.Driver;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static dev.harrel.jarhell.controller.PackagesController.*;
import static org.assertj.core.api.Assertions.assertThat;

@EnvironmentTest
class PackagesControllerTest {
    private final HttpClient httpClient;
    private final Driver driver;

    @Host
    private String host;

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
    void shouldFailForInvalidCoordinates(String coordinate) throws InterruptedException, ExecutionException, TimeoutException {
        String uri = host + "/api/v1/packages/%s".formatted(coordinate);
        ContentResponse res = httpClient.GET(uri);

        assertThat(res.getStatus()).isEqualTo(400);
        ErrorResponse err = TestUtil.readJson(res.getContentAsString(), ErrorResponse.class);
        assertThat(err).isEqualTo(
                new ErrorResponse(uri,
                        HandlerType.GET,
                        "Invalid artifact coordinate format [%s]".formatted(coordinate))
        );
    }

    @Test
    void shouldReturn404ForNotFound() throws InterruptedException, ExecutionException, TimeoutException {
        String uri = host + "/api/v1/packages/org.test:lib:1.0.0";
        ContentResponse res = httpClient.GET(uri);

        assertThat(res.getStatus()).isEqualTo(404);
        ErrorResponse err = TestUtil.readJson(res.getContentAsString(), ErrorResponse.class);
        assertThat(err).isEqualTo(
                new ErrorResponse(uri,
                        HandlerType.GET,
                        "Package with coordinates [org.test:lib:1.0.0] not found")
        );
    }

    @Test
    void shouldFindByGav() throws InterruptedException, ExecutionException, TimeoutException {
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(
                    tx -> tx.run("CREATE (:Artifact {groupId: 'org.test', artifactId: 'lib', version: '1.0.0', classifier: ''})")
            );
        }

        ContentResponse res = httpClient.GET(host + "/api/v1/packages/org.test:lib:1.0.0");

        assertThat(res.getStatus()).isEqualTo(200);
        JsonNode body = TestUtil.readJson(res.getContentAsString(), JsonNode.class);
        assertArtifact(body, "org.test", "lib", "1.0.0", null);
        assertThat(body.get("dependencies").elements()).toIterable().isEmpty();
    }

    @Test
    void shouldFindByGavWithClassifier() throws InterruptedException, ExecutionException, TimeoutException {
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(
                    tx -> tx.run("CREATE (:Artifact {groupId: 'org.test', artifactId: 'lib', version: '1.0.0', classifier: 'doc'})")
            );
        }

        ContentResponse res = httpClient.GET(host + "/api/v1/packages/org.test:lib:1.0.0:doc");

        assertThat(res.getStatus()).isEqualTo(200);
        JsonNode body = TestUtil.readJson(res.getContentAsString(), JsonNode.class);
        assertArtifact(body, "org.test", "lib", "1.0.0", "doc");
        assertThat(body.get("dependencies").elements()).toIterable().isEmpty();
    }

    @Test
    void shouldFindByGavDoesNotReturnWithClassifier() throws InterruptedException, ExecutionException, TimeoutException {
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(
                    tx -> tx.run("CREATE (:Artifact {groupId: 'org.test', artifactId: 'lib', version: '1.0.0', classifier: 'doc'})")
            );
        }

        String uri = host + "/api/v1/packages/org.test:lib:1.0.0";
        ContentResponse res = httpClient.GET(uri);

        assertThat(res.getStatus()).isEqualTo(404);
        ErrorResponse err = TestUtil.readJson(res.getContentAsString(), ErrorResponse.class);
        assertThat(err).isEqualTo(
                new ErrorResponse(uri,
                        HandlerType.GET,
                        "Package with coordinates [org.test:lib:1.0.0] not found")
        );
    }

    @Test
    void shouldFindWithSingleDirectDependency() throws InterruptedException, ExecutionException, TimeoutException {
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(
                    tx -> tx.run("""
                            CREATE (:Artifact {groupId: 'org.test', artifactId: 'lib', version: '1.0.0', classifier: ''})\
                            -[:DEPENDS_ON {optional: false, scope: 'runtime'}]->\
                            (:Artifact {groupId: 'org.test', artifactId: 'dep1', version: '1.0.0', classifier: ''})
                            """)
            );
        }

        String uri = host + "/api/v1/packages/org.test:lib:1.0.0";
        ContentResponse res = httpClient.GET(uri);

        assertThat(res.getStatus()).isEqualTo(200);
        JsonNode body = TestUtil.readJson(res.getContentAsString(), JsonNode.class);
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
    void shouldFindWithMultipleDirectDependencies() throws InterruptedException, ExecutionException, TimeoutException {
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(
                    tx -> tx.run("""
                            CREATE (root:Artifact {groupId: 'org.test', artifactId: 'lib', version: '1.0.0', classifier: ''})\
                            -[:DEPENDS_ON {optional: true, scope: 'runtime'}]->\
                            (:Artifact {groupId: 'org.test', artifactId: 'dep1', version: '1.0.0', classifier: ''}),
                            
                            (root)-[:DEPENDS_ON {optional: false, scope: 'compile'}]->\
                            (:Artifact {groupId: 'org.test', artifactId: 'dep2', version: '1.0.0', classifier: ''})
                            """)
            );
        }

        String uri = host + "/api/v1/packages/org.test:lib:1.0.0";
        ContentResponse res = httpClient.GET(uri);

        assertThat(res.getStatus()).isEqualTo(200);
        JsonNode body = TestUtil.readJson(res.getContentAsString(), JsonNode.class);
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
    void shouldFindWithTransitiveDependencies() throws InterruptedException, ExecutionException, TimeoutException {
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(
                    tx -> tx.run("""
                            CREATE (:Artifact {groupId: 'org.test', artifactId: 'lib', version: '1.0.0', classifier: ''})\
                            -[:DEPENDS_ON {optional: false, scope: 'runtime'}]->\
                            (:Artifact {groupId: 'org.test', artifactId: 'dep1', version: '1.0.0', classifier: ''})
                            -[:DEPENDS_ON {optional: false, scope: 'runtime'}]->\
                            (:Artifact {groupId: 'org.test', artifactId: 'dep2', version: '1.0.0', classifier: ''})
                            """)
            );
        }

        String uri = host + "/api/v1/packages/org.test:lib:1.0.0";
        ContentResponse res = httpClient.GET(uri);

        assertThat(res.getStatus()).isEqualTo(200);
        JsonNode body = TestUtil.readJson(res.getContentAsString(), JsonNode.class);
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
    void shouldFindWithDepth0() throws InterruptedException, ExecutionException, TimeoutException {
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(
                    tx -> tx.run("""
                            CREATE (:Artifact {groupId: 'org.test', artifactId: 'lib', version: '1.0.0', classifier: ''})\
                            -[:DEPENDS_ON {optional: false, scope: 'runtime'}]->\
                            (:Artifact {groupId: 'org.test', artifactId: 'dep1', version: '1.0.0', classifier: ''})
                            -[:DEPENDS_ON {optional: false, scope: 'runtime'}]->\
                            (:Artifact {groupId: 'org.test', artifactId: 'dep2', version: '1.0.0', classifier: ''})
                            """)
            );
        }

        String uri = host + "/api/v1/packages/org.test:lib:1.0.0?depth=0";
        ContentResponse res = httpClient.GET(uri);

        assertThat(res.getStatus()).isEqualTo(200);
        JsonNode body = TestUtil.readJson(res.getContentAsString(), JsonNode.class);
        assertArtifact(body, "org.test", "lib", "1.0.0", null);
        assertThat(body.get("dependencies")).isNull();
    }

    @Test
    void shouldFindWithDepth1() throws InterruptedException, ExecutionException, TimeoutException {
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(
                    tx -> tx.run("""
                            CREATE (:Artifact {groupId: 'org.test', artifactId: 'lib', version: '1.0.0', classifier: ''})\
                            -[:DEPENDS_ON {optional: false, scope: 'runtime'}]->\
                            (:Artifact {groupId: 'org.test', artifactId: 'dep1', version: '1.0.0', classifier: ''})
                            -[:DEPENDS_ON {optional: false, scope: 'runtime'}]->\
                            (:Artifact {groupId: 'org.test', artifactId: 'dep2', version: '1.0.0', classifier: ''})
                            """)
            );
        }

        String uri = host + "/api/v1/packages/org.test:lib:1.0.0?depth=1";
        ContentResponse res = httpClient.GET(uri);

        assertThat(res.getStatus()).isEqualTo(200);
        JsonNode body = TestUtil.readJson(res.getContentAsString(), JsonNode.class);
        assertArtifact(body, "org.test", "lib", "1.0.0", null);

        List<JsonNode> dependencies = TestUtil.iteratorToList(body.get("dependencies").elements());
        assertThat(dependencies).hasSize(1);
        assertThat(dependencies.getFirst().get("optional").asBoolean()).isFalse();
        assertThat(dependencies.getFirst().get("scope").asText()).isEqualTo("runtime");

        JsonNode dep1 = dependencies.getFirst().get("artifact");
        assertArtifact(dep1, "org.test", "dep1", "1.0.0", null);
    }

    @Test
    void shouldReturn400ForInvalidDepth() throws InterruptedException, ExecutionException, TimeoutException {
        String uri = host + "/api/v1/packages/org.test:lib:1.0.0?depth=hello";
        ContentResponse res = httpClient.GET(uri);

        assertThat(res.getStatus()).isEqualTo(400);
        ErrorResponse err = TestUtil.readJson(res.getContentAsString(), ErrorResponse.class);
        assertThat(err).isEqualTo(
                new ErrorResponse(uri,
                        HandlerType.GET,
                        "java.lang.NumberFormatException: For input string: \"hello\"")
        );
    }

    @Test
    void shouldFindAllVersions() throws InterruptedException, ExecutionException, TimeoutException {
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(
                    tx -> tx.run("""
                            CREATE
                            (:Artifact {groupId: 'org.test', artifactId: 'lib', version: '1.0.0', classifier: ''}),
                            (:Artifact {groupId: 'org.test', artifactId: 'lib', version: '1.2.0', classifier: ''})
                            """)
            );
        }

        ContentResponse res = httpClient.GET(host + "/api/v1/packages?groupId=org.test&artifactId=lib");

        assertThat(res.getStatus()).isEqualTo(200);
        ArrayNode body = TestUtil.readJson(res.getContentAsString(), ArrayNode.class);
        assertThat(body.size()).isEqualTo(2);
        assertArtifact(body.get(0), "org.test", "lib", "1.0.0", null);
        assertThat(body.get(0).get("dependencies")).isNull();
        assertArtifact(body.get(1), "org.test", "lib", "1.2.0", null);
        assertThat(body.get(1).get("dependencies")).isNull();
    }

    @Test
    void shouldFindAllVersionsWithClassifier() throws InterruptedException, ExecutionException, TimeoutException {
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(
                    tx -> tx.run("""
                            CREATE
                            (:Artifact {groupId: 'org.test', artifactId: 'lib', version: '1.0.0', classifier: 'doc'}),
                            (:Artifact {groupId: 'org.test', artifactId: 'lib', version: '1.2.0', classifier: 'doc'})
                            """)
            );
        }

        ContentResponse res = httpClient.GET(host + "/api/v1/packages?groupId=org.test&artifactId=lib");

        assertThat(res.getStatus()).isEqualTo(200);
        ArrayNode body = TestUtil.readJson(res.getContentAsString(), ArrayNode.class);
        assertThat(body.size()).isZero();

        res = httpClient.GET(host + "/api/v1/packages?groupId=org.test&artifactId=lib&classifier=doc");

        assertThat(res.getStatus()).isEqualTo(200);
        body = TestUtil.readJson(res.getContentAsString(), ArrayNode.class);
        assertThat(body.size()).isEqualTo(2);
        assertArtifact(body.get(0), "org.test", "lib", "1.0.0", "doc");
        assertThat(body.get(0).get("dependencies")).isNull();
        assertArtifact(body.get(1), "org.test", "lib", "1.2.0", "doc");
        assertThat(body.get(1).get("dependencies")).isNull();
    }

    @Test
    void shouldFailFindAllVersionsWithoutGroupId() throws InterruptedException, ExecutionException, TimeoutException {
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(
                    tx -> tx.run("""
                            CREATE
                            (:Artifact {groupId: 'org.test', artifactId: 'lib'}),
                            (:Artifact {groupId: 'org.test', artifactId: 'lib'})
                            """)
            );
        }

        String uri = host + "/api/v1/packages?artifactId=lib";
        ContentResponse res = httpClient.GET(uri);

        assertThat(res.getStatus()).isEqualTo(400);
        ErrorResponse err = TestUtil.readJson(res.getContentAsString(), ErrorResponse.class);
        assertThat(err).isEqualTo(
                new ErrorResponse(uri,
                        HandlerType.GET,
                        "groupId parameter is required")
        );
    }

    @Test
    void shouldFailFindAllVersionsWithoutArtifactId() throws InterruptedException, ExecutionException, TimeoutException {
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(
                    tx -> tx.run("""
                            CREATE
                            (:Artifact {groupId: 'org.test', artifactId: 'lib'}),
                            (:Artifact {groupId: 'org.test', artifactId: 'lib'})
                            """)
            );
        }

        String uri = host + "/api/v1/packages?groupId=dev.harrel";
        ContentResponse res = httpClient.GET(uri);

        assertThat(res.getStatus()).isEqualTo(400);
        ErrorResponse err = TestUtil.readJson(res.getContentAsString(), ErrorResponse.class);
        assertThat(err).isEqualTo(
                new ErrorResponse(uri,
                        HandlerType.GET,
                        "artifactId parameter is required")
        );
    }

    @Test
    void shouldFailSearchWithoutQuery() throws InterruptedException, ExecutionException, TimeoutException {
        String uri = host + "/api/v1/packages/search";
        ContentResponse res = httpClient.GET(uri);

        assertThat(res.getStatus()).isEqualTo(400);
        ErrorResponse err = TestUtil.readJson(res.getContentAsString(), ErrorResponse.class);
        assertThat(err).isEqualTo(
                new ErrorResponse(uri,
                        HandlerType.GET,
                        "query parameter is required")
        );
    }

    @Test
    void shouldSearchByGroupId() throws InterruptedException, ExecutionException, TimeoutException {
        insertGavs(List.of(
                new Gav("org.test", "lib1", "1.0.0"),
                new Gav("org.test", "lib2", "1.0.0"),
                new Gav("org.hello", "lib1", "1.0.0"),
                new Gav("org.hello", "lib2", "1.0.0")
        ));
        String uri = host + "/api/v1/packages/search?query=test";
        ContentResponse res = httpClient.GET(uri);

        assertThat(res.getStatus()).isEqualTo(200);
        List<SearchResult> found = TestUtil.readJson(res.getContentAsString(), new TypeReference<>() {});
        assertThat(found).containsExactly(
                new SearchResult("org.test", "lib1", "1.0.0"),
                new SearchResult("org.test", "lib2", "1.0.0")
        );
    }

    @Test
    void shouldSearchByArtifactId() throws InterruptedException, ExecutionException, TimeoutException {
        insertGavs(List.of(
                new Gav("org.test", "lib1", "1.0.0"),
                new Gav("org.test", "lib2", "1.0.0"),
                new Gav("org.hello", "lib1", "1.0.0"),
                new Gav("org.hello", "lib2", "1.0.0")
        ));
        String uri = host + "/api/v1/packages/search?query=lib1";
        ContentResponse res = httpClient.GET(uri);

        assertThat(res.getStatus()).isEqualTo(200);
        List<SearchResult> found = TestUtil.readJson(res.getContentAsString(), new TypeReference<>() {});
        assertThat(found).containsExactly(
                new SearchResult("org.test", "lib1", "1.0.0"),
                new SearchResult("org.hello", "lib1", "1.0.0")
        );
    }

    @Test
    void shouldSearchByEitherGroupIdOrArtifactId() throws InterruptedException, ExecutionException, TimeoutException {
        insertGavs(List.of(
                new Gav("org.test", "lib1", "1.0.0"),
                new Gav("org.test", "lib2", "1.0.0"),
                new Gav("org.hello", "lib1", "1.0.0"),
                new Gav("org.hello", "lib2", "1.0.0"),
                new Gav("org.library", "hello", "1.0.0")
        ));
        String uri = host + "/api/v1/packages/search?query=hello";
        ContentResponse res = httpClient.GET(uri);

        assertThat(res.getStatus()).isEqualTo(200);
        List<SearchResult> found = TestUtil.readJson(res.getContentAsString(), new TypeReference<>() {});
        assertThat(found).containsExactly(
                new SearchResult("org.hello", "lib1", "1.0.0"),
                new SearchResult("org.hello", "lib2", "1.0.0"),
                new SearchResult("org.library", "hello", "1.0.0")
        );
    }

    @Test
    void shouldSearchByGroupIdAndArtifactId() throws InterruptedException, ExecutionException, TimeoutException {
        insertGavs(List.of(
                new Gav("org.test", "lib1", "1.0.0"),
                new Gav("org.test", "lib2", "1.0.0"),
                new Gav("org.hello", "lib1", "1.0.0"),
                new Gav("org.hello", "lib2", "1.0.0"),
                new Gav("org.library", "hello", "1.0.0")
        ));
        String uri = host + "/api/v1/packages/search?query=hello:lib";
        ContentResponse res = httpClient.GET(uri);

        assertThat(res.getStatus()).isEqualTo(200);
        List<SearchResult> found = TestUtil.readJson(res.getContentAsString(), new TypeReference<>() {});
        assertThat(found).containsExactly(
                new SearchResult("org.hello", "lib1", "1.0.0"),
                new SearchResult("org.hello", "lib2", "1.0.0")
        );
    }

    @Test
    void shouldSearchPackagesOnlyWithMaxVersion() throws InterruptedException, ExecutionException, TimeoutException {
        insertGavs(List.of(
                new Gav("org.test", "lib1", "1.0.0"),
                new Gav("org.test", "lib1", "1.2.1"),
                new Gav("org.test", "lib1", "2.0.1"),
                new Gav("org.test", "lib2", "1.0.0"),
                new Gav("org.test", "lib2", "1.0.1")
        ));
        String uri = host + "/api/v1/packages/search?query=org.test:lib";
        ContentResponse res = httpClient.GET(uri);

        assertThat(res.getStatus()).isEqualTo(200);
        List<SearchResult> found = TestUtil.readJson(res.getContentAsString(), new TypeReference<>() {});
        assertThat(found).containsExactly(
                new SearchResult("org.test", "lib1", "2.0.1"),
                new SearchResult("org.test", "lib2", "1.0.1")
        );
    }

    @Test
    void shouldSearchUpTo40Records() throws InterruptedException, ExecutionException, TimeoutException {
        insertGavs(IntStream.range(0, 60)
                .mapToObj(i -> new Gav("org.test", "lib" + i, "1.0.0"))
                .toList()
        );
        String uri = host + "/api/v1/packages/search?query=org.test";
        ContentResponse res = httpClient.GET(uri);

        assertThat(res.getStatus()).isEqualTo(200);
        List<SearchResult> found = TestUtil.readJson(res.getContentAsString(), new TypeReference<>() {});
        assertThat(found).hasSize(40);
    }

    void insertGavs(List<Gav> gavs) {
        String statement = gavs.stream()
                .map(gav -> "(:Artifact {groupId: '%s', artifactId: '%s', version: '%s'})"
                        .formatted(gav.groupId(), gav.artifactId(), gav.version()))
                .collect(Collectors.joining(",", "CREATE", ""));
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(tx -> tx.run(statement));
        }
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