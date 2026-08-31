package dev.harrel.jarhell.controller;

import dev.harrel.jarhell.extension.EnvironmentTest;
import dev.harrel.jarhell.extension.Host;
import dev.harrel.jarhell.model.ArtifactInfo;
import dev.harrel.jarhell.model.ArtifactTree;
import dev.harrel.jarhell.model.BytecodeVersion;
import dev.harrel.jarhell.model.DependencyInfo;
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
class CyclicAnalysisTest {
    private final HttpClient httpClient;

    @Host
    private String host;

    CyclicAnalysisTest(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Test
    void hardCycleWithSelfIsIgnored() throws InterruptedException, ExecutionException, TimeoutException {
        ArtifactTree at = analyzeAndWait(new Gav("org.test", "cycle-self", "1.0.0"));

        ArtifactInfo ai = at.artifactInfo();
        assertThat(ai.artifactId()).isEqualTo("cycle-self");
        assertThat(ai.packageSize()).isEqualTo(2105L);
        assertThat(ai.effectiveValues()).usingRecursiveComparison()
                .isEqualTo(effectiveValues(0, 0, 0, 2105L, 1L));
        assertThat(at.dependencies()).isEmpty();
    }

    @Test
    void softCycleWithSelfIsIgnored() throws InterruptedException, ExecutionException, TimeoutException {
        ArtifactTree at = analyzeAndWait(new Gav("org.test", "cycle-self-soft", "1.0.0"));

        ArtifactInfo ai = at.artifactInfo();
        assertThat(ai.artifactId()).isEqualTo("cycle-self-soft");
        assertThat(ai.packageSize()).isEqualTo(2105L);
        assertThat(ai.effectiveValues()).usingRecursiveComparison()
                .isEqualTo(effectiveValues(0, 0, 0, 2105L, 1L));
        assertThat(at.dependencies()).isEmpty();
    }

    @Test
    void hardCycleWithThreeArtifactsIsComputedCorrectly() throws InterruptedException, ExecutionException, TimeoutException {
        ArtifactTree at = analyzeAndWait(new Gav("org.test", "cycle1", "1.0.0"));

        ArtifactInfo ai = at.artifactInfo();
        assertThat(ai.artifactId()).isEqualTo("cycle1");
        assertThat(ai.packageSize()).isEqualTo(2105L);
        assertThat(ai.effectiveValues()).usingRecursiveComparison()
                .isEqualTo(effectiveValues(2, 0, 0, 6315L, 3L));

        assertThat(at.dependencies()).hasSize(1);
        DependencyInfo dep = at.dependencies().getFirst();
        assertThat(dep.optional()).isFalse();
        assertThat(dep.scope()).isEqualTo("compile");

        ArtifactInfo depAi = dep.artifact().artifactInfo();
        assertThat(depAi.artifactId()).isEqualTo("cycle2");
        assertThat(depAi.packageSize()).isEqualTo(2105L);
        assertThat(depAi.effectiveValues()).usingRecursiveComparison()
                .isEqualTo(effectiveValues(2, 0, 0, 6315L, 3L));
    }

    @Test
    void hardCycleWithFourArtifactsIsComputedCorrectly() throws InterruptedException, ExecutionException, TimeoutException {
        ArtifactTree at = analyzeAndWait(new Gav("org.test", "pre-cycle", "1.0.0"));

        ArtifactInfo ai = at.artifactInfo();
        assertThat(ai.artifactId()).isEqualTo("pre-cycle");
        assertThat(ai.packageSize()).isEqualTo(2105L);
        assertThat(ai.effectiveValues()).usingRecursiveComparison()
                .isEqualTo(effectiveValues(0, 0, 3, 2105L, 1L));

        assertThat(at.dependencies()).hasSize(1);
        DependencyInfo dep = at.dependencies().getFirst();
        assertThat(dep.optional()).isTrue();
        assertThat(dep.scope()).isEqualTo("compile");

        ArtifactInfo depAi = dep.artifact().artifactInfo();
        assertThat(depAi.artifactId()).isEqualTo("cycle3");
        assertThat(depAi.packageSize()).isEqualTo(2105L);
        assertThat(depAi.effectiveValues()).usingRecursiveComparison()
                .isEqualTo(effectiveValues(2, 0, 0, 6315L, 3L));
    }

    private ArtifactTree analyzeAndWait(Gav gav) throws InterruptedException, ExecutionException, TimeoutException {
        ContentResponse res = httpClient.newRequest(host + "/api/v1/analyze-and-wait")
                .body(new StringRequestContent(TestUtil.writeJson(gav)))
                .method(HttpMethod.POST)
                .send();

        assertThat(res.getStatus()).isEqualTo(200);
        return TestUtil.readJson(res.getContentAsString(), ArtifactTree.class);
    }

    private ArtifactInfo.EffectiveValues effectiveValues(int requiredDependencies,
                                                         int unresolvedDependencies,
                                                         int optionalDependencies,
                                                         long size,
                                                         long licenseCount) {
        return new ArtifactInfo.EffectiveValues(
                requiredDependencies,
                unresolvedDependencies,
                optionalDependencies,
                size,
                new BytecodeVersion(65, 0),
                LicenseType.NO_LICENSE,
                List.of(Map.entry(LicenseType.NO_LICENSE, licenseCount)));
    }
}
