package dev.harrel.jarhell.analyze;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PackageAnalyzerTest {
    @Test
    void prefersJarOverAnythingElse() {
        assertThat(PackageAnalyzer.selectExtension(Set.of("aar", "jar", "pom", "zip"))).isEqualTo("jar");
    }

    @Test
    void picksPomOnlyWhenNothingElseExists() {
        assertThat(PackageAnalyzer.selectExtension(Set.of("pom", "asc", "module"))).isEqualTo("pom");
    }

    @Test
    void picksAlphabeticallyFirstNonJarFile() {
        assertThat(PackageAnalyzer.selectExtension(Set.of("zip", "tar.gz", "pom"))).isEqualTo("tar.gz");
        assertThat(PackageAnalyzer.selectExtension(Set.of("xml", "json"))).isEqualTo("json");
    }

    @Test
    void ignoresChecksumsSignaturesAndGradleMetadata() {
        assertThat(PackageAnalyzer.selectExtension(Set.of("asc", "sha1", "sha256", "module", "pom", "war"))).isEqualTo("war");
    }

    @Test
    void failsWhenNoArtifactFileExists() {
        assertThatThrownBy(() -> PackageAnalyzer.selectExtension(Set.of("sha1", "asc")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No artifact files");
    }
}
