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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

@EnvironmentTest
@SuppressWarnings("unchecked")
class CyclicAnalysisTest {
    private final HttpClient httpClient;

    @Host
    private String host;

    CyclicAnalysisTest(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Test
    void hardCycleWithSelfIsIgnored() throws InterruptedException, ExecutionException, TimeoutException {
        ContentResponse res = httpClient.newRequest(host + "/api/v1/analyze-and-wait")
                .body(new StringRequestContent(TestUtil.writeJson(
                        new Gav("org.test", "cycle-self", "1.0.0")
                )))
                .method(HttpMethod.POST)
                .send();

        assertThat(res.getStatus()).isEqualTo(200);
        Map<String, Object> properties = TestUtil.readJson(res.getContentAsString(), new TypeReference<>() {});
        assertThat(properties)
                .containsEntry("artifactId", "cycle-self")
                .containsEntry("packageSize", 2105L)
                .containsEntry("effectiveValues", Map.of(
                        "requiredDependencies", 0L,
                        "unresolvedDependencies", 0L,
                        "optionalDependencies", 0L,
                        "size", 2105L,
                        "bytecodeVersion", "65.0",
                        "licenseType", LicenseType.NO_LICENSE.name(),
                        "licenseTypes", List.of(Map.of(LicenseType.NO_LICENSE.name(), 1L))
                ));
        List<Map<String, Object>> deps = (List<Map<String, Object>>) properties.get("dependencies");
        assertThat(deps).isEmpty();
    }

    @Test
    void softCycleWithSelfIsIgnored() throws InterruptedException, ExecutionException, TimeoutException {
        ContentResponse res = httpClient.newRequest(host + "/api/v1/analyze-and-wait")
                .body(new StringRequestContent(TestUtil.writeJson(
                        new Gav("org.test", "cycle-self-soft", "1.0.0")
                )))
                .method(HttpMethod.POST)
                .send();

        assertThat(res.getStatus()).isEqualTo(200);
        Map<String, Object> properties = TestUtil.readJson(res.getContentAsString(), new TypeReference<>() {});
        assertThat(properties).containsEntry("artifactId", "cycle-self-soft")
                .containsEntry("packageSize", 2105L)
                .containsEntry("effectiveValues", Map.of(
                        "requiredDependencies", 0L,
                        "unresolvedDependencies", 0L,
                        "optionalDependencies", 0L,
                        "size", 2105L,
                        "bytecodeVersion", "65.0",
                        "licenseType", LicenseType.NO_LICENSE.name(),
                        "licenseTypes", List.of(Map.of(LicenseType.NO_LICENSE.name(), 1L))
                ));
        List<Map<String, Object>> deps = (List<Map<String, Object>>) properties.get("dependencies");
        assertThat(deps).isEmpty();
    }

    @Test
    void hardCycleWithThreeArtifactsIsComputedCorrectly() throws InterruptedException, ExecutionException, TimeoutException {
        ContentResponse res = httpClient.newRequest(host + "/api/v1/analyze-and-wait")
                .body(new StringRequestContent(TestUtil.writeJson(
                        new Gav("org.test", "cycle1", "1.0.0")
                )))
                .method(HttpMethod.POST)
                .send();

        assertThat(res.getStatus()).isEqualTo(200);
        Map<String, Object> properties = TestUtil.readJson(res.getContentAsString(), new TypeReference<>() {});
        assertThat(properties)
                .containsEntry("artifactId", "cycle1")
                .containsEntry("packageSize", 2105L)
                .containsEntry("effectiveValues", Map.of(
                        "requiredDependencies", 2L,
                        "unresolvedDependencies", 0L,
                        "optionalDependencies", 0L,
                        "size", 6315L,
                        "bytecodeVersion", "65.0",
                        "licenseType", LicenseType.NO_LICENSE.name(),
                        "licenseTypes", List.of(Map.of(LicenseType.NO_LICENSE.name(), 3L))
                ));
        List<Map<String, Object>> deps = (List<Map<String, Object>>) properties.get("dependencies");
        assertThat(deps).hasSize(1);
        assertThat(deps.getFirst())
                .containsEntry("optional", false)
                .containsEntry("scope", "compile");
        assertThat((Map<String, Object>) deps.getFirst().get("artifact"))
                .containsEntry("artifactId", "cycle2")
                .containsEntry("packageSize", 2105L)
                .containsEntry("effectiveValues", Map.of(
                        "requiredDependencies", 2L,
                        "unresolvedDependencies", 0L,
                        "optionalDependencies", 0L,
                        "size", 6315L,
                        "bytecodeVersion", "65.0",
                        "licenseType", LicenseType.NO_LICENSE.name(),
                        "licenseTypes", List.of(Map.of(LicenseType.NO_LICENSE.name(), 3L))
                ));
    }

    @Test
    void hardCycleWithFourArtifactsIsComputedCorrectly() throws InterruptedException, ExecutionException, TimeoutException {
        ContentResponse res = httpClient.newRequest(host + "/api/v1/analyze-and-wait")
                .body(new StringRequestContent(TestUtil.writeJson(
                        new Gav("org.test", "pre-cycle", "1.0.0")
                )))
                .method(HttpMethod.POST)
                .send();

        assertThat(res.getStatus()).isEqualTo(200);
        Map<String, Object> properties = TestUtil.readJson(res.getContentAsString(), new TypeReference<>() {});
        assertThat(properties)
                .containsEntry("artifactId", "pre-cycle")
                .containsEntry("packageSize", 2105L)
                .containsEntry("effectiveValues", Map.of(
                        "requiredDependencies", 0L,
                        "unresolvedDependencies", 0L,
                        "optionalDependencies", 3L,
                        "size", 2105L,
                        "bytecodeVersion", "65.0",
                        "licenseType", LicenseType.NO_LICENSE.name(),
                        "licenseTypes", List.of(Map.of(LicenseType.NO_LICENSE.name(), 1L))
                ));
        List<Map<String, Object>> deps = (List<Map<String, Object>>) properties.get("dependencies");
        assertThat(deps).hasSize(1);
        assertThat(deps.getFirst())
                .containsEntry("optional", true)
                .containsEntry("scope", "compile");
        assertThat((Map<String, Object>) deps.getFirst().get("artifact"))
                .containsEntry("artifactId", "cycle3")
                .containsEntry("packageSize", 2105L)
                .containsEntry("effectiveValues", Map.of(
                        "requiredDependencies", 2L,
                        "unresolvedDependencies", 0L,
                        "optionalDependencies", 0L,
                        "size", 6315L,
                        "bytecodeVersion", "65.0",
                        "licenseType", LicenseType.NO_LICENSE.name(),
                        "licenseTypes", List.of(Map.of(LicenseType.NO_LICENSE.name(), 3L))
                ));
    }
}