package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.MavenApiClient;
import dev.harrel.jarhell.model.ArtifactInfo;
import dev.harrel.jarhell.model.ArtifactInfo.EffectiveValues;
import dev.harrel.jarhell.model.ArtifactTree;
import dev.harrel.jarhell.model.DependencyInfo;
import dev.harrel.jarhell.model.Gav;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.Mockito.mock;

class AnalyzerTest {
    private final Analyzer analyzer = new Analyzer(mock(MavenRunner.class), mock(MavenApiClient.class), mock(PackageAnalyzer.class));

    @ParameterizedTest
    @MethodSource("artifactTrees")
    void name(ArtifactInfo artifactInfo, List<DependencyInfo> deps, EffectiveValues expected) {
        EffectiveValues effectiveValues = analyzer.computeEffectiveValues(artifactInfo, deps);
        assertThat(effectiveValues).isEqualTo(expected);
    }

    private static ArtifactInfo resolved(Long size, String bytecodeVersion) {
        return new ArtifactInfo("org.resolved", "resolved", "1.0.0", null, null,
                size, bytecodeVersion, "jar", "resolved", "desc", null, null, List.of(),
                List.of(), null, null);
    }

    private static ArtifactInfo unresolved() {
        return ArtifactInfo.unresolved(new Gav("org.unresolved", "unresolved", "1.0.0", null));
    }

    private static DependencyInfo resolvedDep(Long size, String bytecodeVersion, boolean optional) {
        return new DependencyInfo(new ArtifactTree(resolved(size, bytecodeVersion), List.of()), optional, "compile");
    }

    private static DependencyInfo unresolvedDep(boolean optional) {
        return new DependencyInfo(new ArtifactTree(unresolved(), List.of()), optional, "compile");
    }

    private static Stream<Arguments> artifactTrees() {
        return Stream.of(
                argumentSet("Standalone package",
                        resolved(1000L, "52.0"), List.of(),
                        new EffectiveValues(0, 0, 0, 1000L, "52.0")
                ),
                argumentSet("Unresolved standalone package",
                        unresolved(), List.of(),
                        null
                ),
                argumentSet("3 required unresolved deps",
                        resolved(1000L, "52.0"), List.of(unresolvedDep(false), unresolvedDep(false), unresolvedDep(false)),
                        new EffectiveValues(3, 3, 0, 1000L, "52.0")
                ),
                argumentSet("1 required unresolved, 2 optional unresolved deps",
                        resolved(1000L, "52.0"), List.of(unresolvedDep(true), unresolvedDep(false), unresolvedDep(true)),
                        new EffectiveValues(3, 3, 2, 1000L, "52.0")
                )
        );
    }
}