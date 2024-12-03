package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.MavenApiClient;
import dev.harrel.jarhell.model.*;
import dev.harrel.jarhell.model.ArtifactInfo.EffectiveValues;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.Mockito.mock;

class AnalyzerTest {
    private final Analyzer analyzer = new Analyzer(mock(MavenRunner.class), mock(MavenApiClient.class), mock(PackageAnalyzer.class));

    @ParameterizedTest
    @MethodSource("artifactTrees")
    void shouldComputeEffectiveValues(ArtifactInfo artifactInfo, List<DependencyInfo> deps, EffectiveValues expected) {
        EffectiveValues effectiveValues = analyzer.computeEffectiveValues(artifactInfo, deps);
        assertThat(effectiveValues).isEqualTo(expected);
    }

    private static ArtifactInfo resolved(Long size, String bytecodeVersion, List<LicenseType> licenseTypes) {
        return new ArtifactInfo("org.resolved", "resolved", "1.0.0", null, null, null,
                size, bytecodeVersion, "jar", "resolved", "desc", null, null, null, null,
                List.of(), licenseTypes, List.of(), null, null);
    }

    private static ArtifactInfo resolved(Long size, String bytecodeVersion) {
        return resolved(size, bytecodeVersion, List.of());
    }

    private static ArtifactInfo unresolved() {
        return ArtifactInfo.unresolved(new Gav("org.unresolved", "unresolved", "1.0.0", null));
    }

    private static DependencyInfo resolvedDep(Long size, String bytecodeVersion, boolean optional) {
        return new DependencyInfo(new ArtifactTree(resolved(size, bytecodeVersion), List.of()), optional, "compile");
    }

    private static DependencyInfo resolvedDep(Long size, String bytecodeVersion, boolean optional, List<LicenseType> licenseTypes) {
        return new DependencyInfo(new ArtifactTree(resolved(size, bytecodeVersion, licenseTypes), List.of()), optional, "compile");
    }

    private static DependencyInfo unresolvedDep(boolean optional) {
        return new DependencyInfo(new ArtifactTree(unresolved(), List.of()), optional, "compile");
    }

    private static Stream<Arguments> artifactTrees() {
        return Stream.of(
                argumentSet("Standalone package",
                        resolved(1000L, "52.0"), List.of(),
                        new EffectiveValues(0, 0, 0, 1000L, "52.0",
                                LicenseType.NO_LICENSE, List.of(Map.entry(LicenseType.NO_LICENSE, 1L)))
                ),
                argumentSet("Standalone package, single license",
                        resolved(1000L, "52.0", List.of(LicenseType.APACHE_2)), List.of(),
                        new EffectiveValues(0, 0, 0, 1000L, "52.0",
                                LicenseType.APACHE_2, List.of(Map.entry(LicenseType.APACHE_2, 1L)))
                ),
                argumentSet("Standalone package, multiple licenses",
                        resolved(1000L, "52.0", List.of(LicenseType.APACHE_2, LicenseType.CPL_1, LicenseType.GPL_3)), List.of(),
                        new EffectiveValues(0, 0, 0, 1000L, "52.0",
                                LicenseType.GPL_3, List.of(
                                Map.entry(LicenseType.GPL_3, 1L),
                                Map.entry(LicenseType.CPL_1, 1L),
                                Map.entry(LicenseType.APACHE_2, 1L)
                        ))
                ),
                argumentSet("Unresolved standalone package",
                        unresolved(), List.of(),
                        null
                ),
                argumentSet("3 required unresolved deps",
                        resolved(1000L, "52.0"), List.of(unresolvedDep(false), unresolvedDep(false), unresolvedDep(false)),
                        new EffectiveValues(3, 3, 0, 1000L, "52.0",
                                LicenseType.NO_LICENSE, List.of(Map.entry(LicenseType.NO_LICENSE, 1L)))
                ),
                argumentSet("1 required unresolved, 2 optional unresolved deps",
                        resolved(1000L, "52.0"), List.of(unresolvedDep(true), unresolvedDep(false), unresolvedDep(true)),
                        new EffectiveValues(1, 1, 2, 1000L, "52.0",
                                LicenseType.NO_LICENSE, List.of(Map.entry(LicenseType.NO_LICENSE, 1L)))
                ),
                argumentSet("3 required resolved deps - size is calculated properly",
                        resolved(1000L, "52.0"), List.of(
                                resolvedDep(1L, "52.0", false),
                                resolvedDep(10L, "52.0", false),
                                resolvedDep(100L, "52.0", false)
                        ),
                        new EffectiveValues(3, 0, 0, 1111L, "52.0",
                                LicenseType.NO_LICENSE, List.of(Map.entry(LicenseType.NO_LICENSE, 4L)))
                ),
                argumentSet("3 required resolved deps - licenses are calculated properly",
                        resolved(1000L, "52.0", List.of(LicenseType.APACHE_2)), List.of(
                                resolvedDep(1L, "52.0", false, List.of(LicenseType.APACHE_2)),
                                resolvedDep(10L, "52.0", false, List.of(LicenseType.APACHE_2)),
                                resolvedDep(100L, "52.0", false, List.of(LicenseType.MIT))
                        ),
                        new EffectiveValues(3, 0, 0, 1111L, "52.0",
                                LicenseType.APACHE_2, List.of(
                                        Map.entry(LicenseType.APACHE_2, 3L),
                                        Map.entry(LicenseType.MIT, 1L)
                        ))
                ),
                argumentSet("3 required, 1 optional resolved deps - size is calculated properly",
                        resolved(1000L, "52.0"), List.of(
                                resolvedDep(1L, "52.0", false),
                                resolvedDep(10L, "52.0", false),
                                resolvedDep(9999L, "52.0", true),
                                resolvedDep(100L, "52.0", false)
                        ),
                        new EffectiveValues(3, 0, 1, 1111L, "52.0",
                                LicenseType.NO_LICENSE, List.of(Map.entry(LicenseType.NO_LICENSE, 4L)))
                ),
                argumentSet("3 required, 1 optional resolved deps - bytecode version is calculated properly",
                        resolved(1000L, "52.0"), List.of(
                                resolvedDep(10L, "65.0", true),
                                resolvedDep(1L, "61.0", false),
                                resolvedDep(10L, "61.65536", false),
                                resolvedDep(100L, "52.0", false)
                        ),
                        new EffectiveValues(3, 0, 1, 1111L, "61.65536",
                                LicenseType.NO_LICENSE, List.of(Map.entry(LicenseType.NO_LICENSE, 4L)))
                ),
                argumentSet("3 required, 1 optional resolved deps - licenses are calculated properly",
                        resolved(1000L, "52.0", List.of(LicenseType.LGPL_3)), List.of(
                                resolvedDep(1L, "52.0", false, List.of(LicenseType.MPL_2)),
                                resolvedDep(10L, "52.0", false, List.of(LicenseType.MIT0)),
                                resolvedDep(9999L, "52.0", true, List.of(LicenseType.AGPL_3)),
                                resolvedDep(100L, "52.0", false, List.of(LicenseType.MIT))
                        ),
                        new EffectiveValues(3, 0, 1, 1111L, "52.0",
                                LicenseType.LGPL_3, List.of(
                                Map.entry(LicenseType.LGPL_3, 1L),
                                Map.entry(LicenseType.MPL_2, 1L),
                                Map.entry(LicenseType.MIT, 1L),
                                Map.entry(LicenseType.MIT0, 1L)
                        ))
                ),
                argumentSet("All cases combined",
                        resolved(1000L, "68.0", List.of(LicenseType.MIT)), List.of(
                                unresolvedDep(true),
                                resolvedDep(10L, "65.0", true),
                                unresolvedDep(false),
                                resolvedDep(1L, "61.0", false),
                                resolvedDep(10L, "61.65536", false, List.of(LicenseType.MIT)),
                                resolvedDep(100L, "52.0", false, List.of(LicenseType.MIT))
                        ),
                        new EffectiveValues(4, 1, 2, 1111L, "68.0",
                                LicenseType.NO_LICENSE, List.of(
                                        Map.entry(LicenseType.NO_LICENSE, 1L),
                                        Map.entry(LicenseType.MIT, 3L)
                        ))
                )
        );
    }
}