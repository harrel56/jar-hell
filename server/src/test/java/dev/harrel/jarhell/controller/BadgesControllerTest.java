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
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
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
        when(artifactTree.artifactInfo()).thenReturn(artifactInfo);
    }

    @ParameterizedTest
    @ValueSource(strings = {"hello", "a:b:c:d", "", ":", "::", ":::", "::::"})
    void throwsForInvalidCoordinate(String cord) {
        assertThatThrownBy(() -> badgesController.size(ctx, cord))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid artifact coordinate format [%s]".formatted(cord));
    }

    @Test
    void failsForInvalidVersion() {
        badgesController.size(ctx, "org.test:lib:0.0.1");

        verify(ctx).header(Header.CACHE_CONTROL, "max-age=604800");
        verify(ctx).redirect("https://shields.io/badge/package_size-not_found-red", HttpStatus.SEE_OTHER);
    }

    @Test
    void failsForNotAnalyzedVersion() {
        when(mavenApiClient.checkIfArtifactExists(any())).thenReturn(true);
        badgesController.size(ctx, "org.test:lib:0.0.1");

        verify(ctx).header(Header.CACHE_CONTROL, "max-age=300");
        verify(ctx).redirect("https://shields.io/badge/package_size-not_analyzed-yellow", HttpStatus.SEE_OTHER);
    }

    @Test
    void failsForUnresolvedVersion() {
        when(artifactInfo.unresolved()).thenReturn(true);
        when(repo.find(any(), anyInt())).thenReturn(Optional.of(artifactTree));
        badgesController.size(ctx, "org.test:lib:0.0.1");

        verify(ctx).header(Header.CACHE_CONTROL, "max-age=86400");
        verify(ctx).redirect("https://shields.io/badge/package_size-analysis_failed-red", HttpStatus.SEE_OTHER);
    }

    @Test
    void failsForUnknownArtifact() {
        badgesController.size(ctx, "org.test:lib");

        verify(ctx).header(Header.CACHE_CONTROL, "max-age=604800");
        verify(ctx).redirect("https://shields.io/badge/package_size-not_found-red", HttpStatus.SEE_OTHER);
    }

    @Test
    void findsLatestArtifactVersion() {
        when(mavenApiClient.fetchArtifactVersions("org.test", "lib")).thenReturn(List.of("1.0.0", "2.0.0", "2.1.0"));
        when(artifactInfo.packageSize()).thenReturn(123_321L);
        when(repo.find(new Gav("org.test", "lib", "2.1.0"), 0)).thenReturn(Optional.of(artifactTree));
        badgesController.size(ctx, "org.test:lib");

        verify(ctx).header(Header.CACHE_CONTROL, "max-age=604800");
        verify(ctx).redirect("https://shields.io/badge/package_size-123.32KB-brightgreen", HttpStatus.SEE_OTHER);
    }
}