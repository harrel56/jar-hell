package dev.harrel.jarhell.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import dev.harrel.jarhell.extension.EnvironmentTest;
import dev.harrel.jarhell.extension.Host;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.model.LicenseType;
import dev.harrel.jarhell.util.TestUtil;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringRequestContent;
import org.eclipse.jetty.http.HttpMethod;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.EagerResult;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

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
    void shouldAnalyzeStandaloneLib() throws InterruptedException, ExecutionException, TimeoutException {
        ContentResponse res = httpClient.newRequest(host + "/api/v1/analyze")
                .body(new StringRequestContent(TestUtil.writeJson(
                        new Gav("com.sanctionco.jmail", "jmail", "1.6.2")
                )))
                .method(HttpMethod.POST)
                .send();

        assertThat(res.getStatus()).isEqualTo(202);
        assertThat(res.getContentAsString()).isEmpty();

        await().atMost(Duration.ofSeconds(5)).until(() -> !fetchByArtifactId("jmail").records().isEmpty());

        ContentResponse packageRes = httpClient.GET(host + "/api/v1/packages/com.sanctionco.jmail:jmail:1.6.2");
        assertThat(packageRes.getStatus()).isEqualTo(200);
        Map<String, Object> properties = TestUtil.readJson(packageRes.getContentAsString(), new TypeReference<>() {});
        assertThat(properties).isNotNull();
        assertThat(properties).containsEntry("licenses", List.of(Map.of(
                "name", "MIT License",
                "url", "https://opensource.org/licenses/mit-license.php"
        )));
        assertThat(properties).containsEntry("dependencies", List.of());
        assertJmailArtifactInfo(properties);
    }

    @Test
    void shouldAnalyzeUnresolvedLib() throws InterruptedException, ExecutionException, TimeoutException {
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(tx -> {
                tx.run("CREATE (:Artifact {groupId: 'com.sanctionco.jmail', artifactId: 'jmail', version: '1.6.2', classifier: '', unresolved: true})");
            });
        }
        ContentResponse res = httpClient.newRequest(host + "/api/v1/analyze")
                .body(new StringRequestContent(TestUtil.writeJson(
                        new Gav("com.sanctionco.jmail", "jmail", "1.6.2")
                )))
                .method(HttpMethod.POST)
                .send();

        assertThat(res.getStatus()).isEqualTo(202);
        assertThat(res.getContentAsString()).isEmpty();

        await().atMost(Duration.ofSeconds(5)).until(() -> !fetchByArtifactId("jmail").records().isEmpty());

        ContentResponse packageRes = httpClient.GET(host + "/api/v1/packages/com.sanctionco.jmail:jmail:1.6.2");
        assertThat(packageRes.getStatus()).isEqualTo(200);
        Map<String, Object> properties = TestUtil.readJson(packageRes.getContentAsString(), new TypeReference<>() {});
        assertThat(properties).isNotNull();
        assertThat(properties).containsEntry("licenses", List.of(Map.of(
                "name", "MIT License",
                "url", "https://opensource.org/licenses/mit-license.php"
        )));
        assertThat(properties).containsEntry("dependencies", List.of());
        assertJmailArtifactInfo(properties);
    }

    @Test
    void shouldAnalyzeLibWithDependency() throws InterruptedException, ExecutionException, TimeoutException {
        ContentResponse res = httpClient.newRequest(host + "/api/v1/analyze")
                .body(new StringRequestContent(TestUtil.writeJson(
                        new Gav("org.test", "artifact", "3.0.1")
                )))
                .method(HttpMethod.POST)
                .send();

        assertThat(res.getStatus()).isEqualTo(202);
        assertThat(res.getContentAsString()).isEmpty();

        await().atMost(Duration.ofSeconds(5)).until(() -> !fetchByArtifactId("artifact").records().isEmpty());

        ContentResponse packageRes = httpClient.GET(host + "/api/v1/packages/com.sanctionco.jmail:jmail:1.6.2");
        assertThat(packageRes.getStatus()).isEqualTo(200);
        Map<String, Object> properties = TestUtil.readJson(packageRes.getContentAsString(), new TypeReference<>() {});
        assertThat(properties).isNotNull();
        assertThat(properties).containsEntry("licenses", List.of(Map.of(
                "name", "MIT License",
                "url", "https://opensource.org/licenses/mit-license.php"
        )));
        assertThat(properties).containsEntry("dependencies", List.of());
        assertJmailArtifactInfo(properties);
    }

    @Test
    void shouldAnalyzeAndWaitForStandaloneLib() throws InterruptedException, ExecutionException, TimeoutException {
        ContentResponse res = httpClient.newRequest(host + "/api/v1/analyze-and-wait")
                .body(new StringRequestContent(TestUtil.writeJson(
                        new Gav("com.sanctionco.jmail", "jmail", "1.6.2")
                )))
                .method(HttpMethod.POST)
                .send();

        assertThat(res.getStatus()).isEqualTo(200);
        Map<String, Object> properties = TestUtil.readJson(res.getContentAsString(), new TypeReference<>() {});
        assertThat(properties).isNotNull();
        assertThat(properties).containsEntry("licenses", List.of(Map.of(
                "name", "MIT License",
                "url", "https://opensource.org/licenses/mit-license.php"
        )));
        assertThat(properties).containsEntry("dependencies", List.of());
        assertJmailArtifactInfo(properties);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldAnalyzeAndWaitForLibWithDependency() throws InterruptedException, ExecutionException, TimeoutException {
        ContentResponse res = httpClient.newRequest(host + "/api/v1/analyze-and-wait")
                .body(new StringRequestContent(TestUtil.writeJson(
                        new Gav("org.test", "artifact", "3.0.1")
                )))
                .method(HttpMethod.POST)
                .send();

        assertThat(res.getStatus()).isEqualTo(200);
        Map<String, Object> properties = TestUtil.readJson(res.getContentAsString(), new TypeReference<>() {});
        assertThat(properties).isNotNull();
        assertTestArtifactInfo(properties);
        var dependencies = (List<Map<String, Object>>) properties.get("dependencies");
        assertThat(dependencies).hasSize(1);
        assertThat(dependencies.getFirst()).containsEntry("optional", false);
        assertThat(dependencies.getFirst()).containsEntry("scope", "compile");

        assertJmailArtifactInfo(((Map<String, Object>) dependencies.getFirst().get("artifact")));
    }

    private EagerResult fetchByArtifactId(String id) {
        return driver.executableQuery("MATCH (n) WHERE n.artifactId = '%s' AND n.unresolved IS NULL RETURN n".formatted(id)).execute();
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
                Map.entry("licenseTypes", List.of(LicenseType.MIT.name())),
                Map.entry("classifiers", List.of("javadoc", "sources")),
                Map.entry("effectiveValues", Map.of(
                        "requiredDependencies", 0L,
                        "unresolvedDependencies", 0L,
                        "optionalDependencies", 0L,
                        "size", 30629L,
                        "bytecodeVersion", "52.0",
                        "licenseType", LicenseType.MIT.name(),
                        "licenseTypes", List.of(Map.of(LicenseType.MIT.name(), 1L))
                ))
        );
    }

    private void assertTestArtifactInfo(Map<String, Object> properties) {
        assertThat(properties).contains(
                Map.entry("groupId", "org.test"),
                Map.entry("artifactId", "artifact"),
                Map.entry("version", "3.0.1"),
                Map.entry("name", "artifact"),
                Map.entry("description", "Artifact for tests"),
                Map.entry("packaging", "jar"),
                Map.entry("packageSize", 2105L),
                Map.entry("bytecodeVersion", "65.0"),
                Map.entry("licenseTypes", List.of()),
                Map.entry("classifiers", List.of("javadoc", "sources")),
                Map.entry("effectiveValues", Map.of(
                        "requiredDependencies", 1L,
                        "unresolvedDependencies", 0L,
                        "optionalDependencies", 0L,
                        "size", 32734L,
                        "bytecodeVersion", "65.0",
                        "licenseType", LicenseType.NO_LICENSE.name(),
                        "licenseTypes", List.of(Map.of(LicenseType.NO_LICENSE.name(), 1L), Map.of(LicenseType.MIT.name(), 1L))
                ))
        );
    }
}