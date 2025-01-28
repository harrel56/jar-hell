package dev.harrel.jarhell.controller;

import dev.harrel.jarhell.MavenApiClient;
import dev.harrel.jarhell.analyze.AnalyzeEngine;
import dev.harrel.jarhell.error.BadRequestException;
import dev.harrel.jarhell.model.ArtifactInfo;
import dev.harrel.jarhell.model.ArtifactTree;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.repo.ArtifactRepository;
import io.javalin.http.Context;
import io.javalin.http.Header;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BadgesControllerTest {
    private final ArtifactRepository repo = mock(ArtifactRepository.class);
    private final MavenApiClient mavenApiClient = mock(MavenApiClient.class);
    private final AnalyzeEngine engine = mock(AnalyzeEngine.class);
    private final BadgesController badgesController = new BadgesController(repo, mavenApiClient, engine);
    private final Context ctx = mock(Context.class);
    private final ArtifactTree artifactTree = mock(ArtifactTree.class);
    private final ArtifactInfo artifactInfo = mock(ArtifactInfo.class);

    BadgesControllerTest() {
        when(ctx.header(any(), any())).thenReturn(ctx);
        when(ctx.queryParamMap()).thenReturn(Map.of("color", List.of("pink")));
        when(artifactTree.artifactInfo()).thenReturn(artifactInfo);
    }

    @ParameterizedTest
    @ValueSource(strings = {"hello", "a:b:c:d", "", ":", "::", ":::", "::::"})
    void throwsForInvalidCoordinate(String cord) {
        assertThatThrownBy(() -> badgesController.getMetricBadge(ctx, BadgesController.Metric.size, cord))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid artifact coordinate format [%s]".formatted(cord));
    }

    @ParameterizedTest
    @EnumSource(BadgesController.Metric.class)
    void failsForInvalidVersion(BadgesController.Metric metric) {
        badgesController.getMetricBadge(ctx, metric, "org.test:lib:0.0.1");

        verify(ctx).header(Header.CACHE_CONTROL, "max-age=604800");
        verify(ctx).redirect(
                startsWith("https://shields.io/badge/%s-not_found-red?logo=data:image/png;base64,".formatted(escapedName(metric))),
                eq(HttpStatus.SEE_OTHER));
    }

    @ParameterizedTest
    @EnumSource(BadgesController.Metric.class)
    void failsForNotAnalyzedVersion(BadgesController.Metric metric) {
        when(mavenApiClient.checkIfArtifactExists(any())).thenReturn(true);
        badgesController.getMetricBadge(ctx, metric, "org.test:lib:0.0.1");

        verify(ctx).header(Header.CACHE_CONTROL, "max-age=300");
        verify(ctx).redirect(
                startsWith("https://shields.io/badge/%s-not_analyzed-yellow?logo=data:image/png;base64,".formatted(escapedName(metric))),
                eq(HttpStatus.SEE_OTHER));
    }

    @ParameterizedTest
    @EnumSource(BadgesController.Metric.class)
    void failsForUnresolvedVersion(BadgesController.Metric metric) {
        when(artifactInfo.unresolved()).thenReturn(true);
        when(repo.find(any(), anyInt())).thenReturn(Optional.of(artifactTree));
        badgesController.getMetricBadge(ctx, metric, "org.test:lib:0.0.1");

        verify(ctx).header(Header.CACHE_CONTROL, "max-age=86400");
        verify(ctx).redirect(
                startsWith("https://shields.io/badge/%s-analysis_failed-red?logo=data:image/png;base64,".formatted(escapedName(metric))),
                eq(HttpStatus.SEE_OTHER));
    }

    @ParameterizedTest
    @EnumSource(BadgesController.Metric.class)
    void failsForUnknownArtifact(BadgesController.Metric metric) {
        badgesController.getMetricBadge(ctx, metric, "org.test:lib");

        verify(ctx).header(Header.CACHE_CONTROL, "max-age=604800");
        verify(ctx).redirect(
                startsWith("https://shields.io/badge/%s-not_found-red?logo=data:image/png;base64,".formatted(escapedName(metric))),
                eq(HttpStatus.SEE_OTHER));
    }

    @Test
    void findsLatestArtifactVersionForTotalSize() {
        ArtifactInfo.EffectiveValues effectiveValues = mock(ArtifactInfo.EffectiveValues.class);
        when(mavenApiClient.fetchArtifactVersions("org.test", "lib")).thenReturn(List.of("1.0.0", "2.0.0", "2.1.0"));
        when(effectiveValues.size()).thenReturn(123_321L);
        when(artifactInfo.effectiveValues()).thenReturn(effectiveValues);
        when(repo.find(new Gav("org.test", "lib", "2.1.0"), 0)).thenReturn(Optional.of(artifactTree));
        badgesController.getMetricBadge(ctx, BadgesController.Metric.total_size, "org.test:lib");

        verify(ctx).header(Header.CACHE_CONTROL, "max-age=604800");
        verify(ctx).redirect(
                startsWith("https://shields.io/badge/total_size-123.32KB-brightgreen?color=pink&logo=data:image/png;base64,"),
                eq(HttpStatus.SEE_OTHER));
    }

    @Test
    void findsLatestArtifactVersionForEffectiveBytecode() {
        ArtifactInfo.EffectiveValues effectiveValues = mock(ArtifactInfo.EffectiveValues.class);
        when(mavenApiClient.fetchArtifactVersions("org.test", "lib")).thenReturn(List.of("1.0.0", "2.0.0", "2.1.0"));
        when(effectiveValues.bytecodeVersion()).thenReturn("52.0");
        when(artifactInfo.effectiveValues()).thenReturn(effectiveValues);
        when(repo.find(new Gav("org.test", "lib", "2.1.0"), 0)).thenReturn(Optional.of(artifactTree));
        badgesController.getMetricBadge(ctx, BadgesController.Metric.effective_bytecode, "org.test:lib");

        verify(ctx).header(Header.CACHE_CONTROL, "max-age=604800");
        verify(ctx).redirect(
                startsWith("https://shields.io/badge/effective_bytecode_version-java_8-brightgreen?color=pink&logo=data:image/png;base64,"),
                eq(HttpStatus.SEE_OTHER));
    }

    @Test
    void shouldOverrideLogo() {
        when(ctx.queryParamMap()).thenReturn(Map.of("logo", List.of("fireship")));
        when(artifactInfo.packageSize()).thenReturn(1_654_321L);
        when(repo.find(new Gav("org.test", "lib", "0.0.1"), 0)).thenReturn(Optional.of(artifactTree));

        badgesController.getMetricBadge(ctx, BadgesController.Metric.size, "org.test:lib:0.0.1");

        verify(ctx).header(Header.CACHE_CONTROL, "max-age=604800");
        verify(ctx).redirect("https://shields.io/badge/package_size-1.65MB-orange?logo=fireship", HttpStatus.SEE_OTHER);
    }

    @Test
    void shouldEraseLogo() {
        when(ctx.queryParamMap()).thenReturn(Map.of("logo", List.of("")));
        when(artifactInfo.packageSize()).thenReturn(1_654_321L);
        when(repo.find(new Gav("org.test", "lib", "0.0.1"), 0)).thenReturn(Optional.of(artifactTree));

        badgesController.getMetricBadge(ctx, BadgesController.Metric.size, "org.test:lib:0.0.1");

        verify(ctx).header(Header.CACHE_CONTROL, "max-age=604800");
        verify(ctx).redirect("https://shields.io/badge/package_size-1.65MB-orange?logo=", HttpStatus.SEE_OTHER);
    }

    @Test
    void shouldPassAlongManyParams() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("style", List.of("plastic"));
        map.put("label", List.of("hello"));
        map.put("param", List.of("1", "2", "3"));
        when(ctx.queryParamMap()).thenReturn(map);
        when(artifactInfo.packageSize()).thenReturn(1_654_321L);
        when(repo.find(new Gav("org.test", "lib", "0.0.1"), 0)).thenReturn(Optional.of(artifactTree));

        badgesController.getMetricBadge(ctx, BadgesController.Metric.size, "org.test:lib:0.0.1");

        verify(ctx).header(Header.CACHE_CONTROL, "max-age=604800");
        verify(ctx).redirect(
                startsWith("https://shields.io/badge/package_size-1.65MB-orange?style=plastic&label=hello&param=1,2,3&logo=data:image/png;base64,"),
                eq(HttpStatus.SEE_OTHER));
    }

    private static String escapedName(BadgesController.Metric metric) {
        return metric.getName().replace(' ', '_');
    }
}