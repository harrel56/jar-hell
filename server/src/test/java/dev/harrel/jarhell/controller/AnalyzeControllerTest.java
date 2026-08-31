package dev.harrel.jarhell.controller;

import dev.harrel.jarhell.analyze.JarAnalyzer;
import dev.harrel.jarhell.extension.EnvironmentTest;
import dev.harrel.jarhell.extension.Host;
import dev.harrel.jarhell.model.*;
import dev.harrel.jarhell.model.descriptor.License;
import dev.harrel.jarhell.util.TestUtil;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringRequestContent;
import org.eclipse.jetty.http.HttpMethod;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.EagerResult;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        ArtifactTree at = TestUtil.readJson(packageRes.getContentAsString(), ArtifactTree.class);

        assertThat(at).usingRecursiveComparison()
                .ignoringFieldsOfTypes(LocalDateTime.class)
                .isEqualTo(jmailArtifact());
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
        ArtifactTree at = TestUtil.readJson(packageRes.getContentAsString(), ArtifactTree.class);

        assertThat(at).usingRecursiveComparison()
                .ignoringFieldsOfTypes(LocalDateTime.class)
                .isEqualTo(jmailArtifact());
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

        await().atMost(Duration.ofSeconds(5)).until(() -> !fetchByArtifactId("jmail").records().isEmpty());

        ContentResponse packageRes = httpClient.GET(host + "/api/v1/packages/com.sanctionco.jmail:jmail:1.6.2");
        assertThat(packageRes.getStatus()).isEqualTo(200);
        ArtifactTree at = TestUtil.readJson(packageRes.getContentAsString(), ArtifactTree.class);

        assertThat(at).usingRecursiveComparison()
                .ignoringFieldsOfTypes(LocalDateTime.class)
                .isEqualTo(jmailArtifact());
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
        ArtifactTree at = TestUtil.readJson(res.getContentAsString(), ArtifactTree.class);

        assertThat(at).usingRecursiveComparison()
                .ignoringFieldsOfTypes(LocalDateTime.class)
                .isEqualTo(jmailArtifact());
    }

    @Test
    void shouldAnalyzeAndWaitForLibWithDependency() throws InterruptedException, ExecutionException, TimeoutException {
        ContentResponse res = httpClient.newRequest(host + "/api/v1/analyze-and-wait")
                .body(new StringRequestContent(TestUtil.writeJson(
                        new Gav("org.test", "artifact", "3.0.1")
                )))
                .method(HttpMethod.POST)
                .send();

        assertThat(res.getStatus()).isEqualTo(200);
        ArtifactTree at = TestUtil.readJson(res.getContentAsString(), ArtifactTree.class);

        assertThat(at).usingRecursiveComparison()
                .ignoringFieldsOfTypes(LocalDateTime.class)
                .isEqualTo(testArtifact());
    }

    private EagerResult fetchByArtifactId(String id) {
        return driver.executableQuery("MATCH (n) WHERE n.artifactId = '%s' AND n.unresolved IS NULL RETURN n".formatted(id)).execute();
    }

    private ArtifactTree jmailArtifact() {
        JarAnalyzer.JarInfo jarInfo = new JarAnalyzer.JarInfo(
                Map.of(
                        JarAnalyzer.ContentType.JAVA, new JarAnalyzer.Content(16, 49197L, 23678L),
                        JarAnalyzer.ContentType.RESOURCE, new JarAnalyzer.Content(3, 10988L, 2422L)
                ),
                Map.of(
                        JarAnalyzer.ClassType.CLASS, 11,
                        JarAnalyzer.ClassType.ENUM, 1
                ),
                2,
                new BytecodeVersion(52, 0),
                "21",
                false,
                false,
                Set.of(),
                JarAnalyzer.ModuleType.NAMED,
                "com.sanctionco.jmail");

        ArtifactInfo.EffectiveValues effectiveValues = new ArtifactInfo.EffectiveValues(
                0,
                0,
                0,
                30629L,
                new BytecodeVersion(52, 0),
                LicenseType.MIT,
                List.of(Map.entry(LicenseType.MIT, 1L)));

        ArtifactInfo ai = new ArtifactInfo(
                "com.sanctionco.jmail",
                "jmail",
                "1.6.2",
                "",
                null, null, null, LocalDateTime.now(),
                30629L,
                "jar",
                "jmail",
                "A modern, fast, zero-dependency library for working with emails in Java",
                "https://github.com/RohanNagar/jmail",
                "https://github.com/RohanNagar/jmail",
                null,
                null,
                List.of(new License("MIT License", "https://opensource.org/licenses/mit-license.php")),
                List.of(LicenseType.MIT),
                List.of("javadoc", "sources"),
                List.of("jar", "pom"),
                jarInfo,
                effectiveValues,
                LocalDateTime.now());

        return new ArtifactTree(ai, List.of());
    }

    private ArtifactTree testArtifact() {
        JarAnalyzer.JarInfo jarInfo = new JarAnalyzer.JarInfo(
                Map.of(
                        JarAnalyzer.ContentType.JAVA, new JarAnalyzer.Content(1, 540L, 338L),
                        JarAnalyzer.ContentType.RESOURCE, new JarAnalyzer.Content(3, 920L, 415L)
                ),
                Map.of(JarAnalyzer.ClassType.CLASS, 1),
                0,
                new BytecodeVersion(65, 0),
                "21",
                false,
                false,
                Set.of(),
                JarAnalyzer.ModuleType.UNNAMED,
                null);

        ArtifactInfo.EffectiveValues effectiveValues = new ArtifactInfo.EffectiveValues(
                1,
                0,
                0,
                32734L,
                new BytecodeVersion(65, 0),
                LicenseType.NO_LICENSE,
                List.of(Map.entry(LicenseType.NO_LICENSE, 1L), Map.entry(LicenseType.MIT, 1L)));

        ArtifactInfo ai = new ArtifactInfo(
                "org.test",
                "artifact",
                "3.0.1",
                "",
                null, null, null, LocalDateTime.now(),
                2105L,
                "jar",
                "artifact",
                "Artifact for tests",
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of("javadoc", "sources"),
                List.of("jar", "pom"),
                jarInfo,
                effectiveValues,
                LocalDateTime.now());

        // leaf dependencies are not expanded any further, hence null instead of an empty list
        ArtifactTree jmail = new ArtifactTree(jmailArtifact().artifactInfo(), null);
        return new ArtifactTree(ai, List.of(new DependencyInfo(jmail, false, "compile")));
    }
}