package dev.harrel.jarhell.repo;

import dev.harrel.jarhell.extension.EnvironmentTest;
import dev.harrel.jarhell.model.ArtifactInfo;
import dev.harrel.jarhell.model.ArtifactTree;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.model.LicenseType;
import dev.harrel.jarhell.model.descriptor.License;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@EnvironmentTest
class ArtifactRepositoryTest {
    private final ArtifactRepository repo;

    ArtifactRepositoryTest(ArtifactRepository repo) {
        this.repo = repo;
    }

    @Test
    void shouldCreateNewResolvedArtifact() {
        Gav gav = new Gav("x", "y", "1");
        ArtifactInfo artifact = artifactInfo(gav);
        repo.saveArtifact(artifact);
        Optional<ArtifactTree> artifactTree = repo.find(gav);

        assertThat(artifactTree).isPresent();
        assertThat(artifactTree.get().dependencies()).isEmpty();
        assertThat(artifactTree.get().artifactInfo().analyzed()).isBeforeOrEqualTo(LocalDateTime.now());
        assertArtifact(artifactTree.get().artifactInfo(), gav);
    }

    @Test
    void creationShouldBeIdempotent() {
        Gav gav = new Gav("x", "y", "1");
        ArtifactInfo artifact = artifactInfo(gav);
        repo.saveArtifact(artifact);
        repo.saveArtifact(artifact);
        Optional<ArtifactTree> artifactTree = repo.find(gav);

        assertThat(artifactTree).isPresent();
        assertThat(artifactTree.get().dependencies()).isEmpty();
        assertThat(artifactTree.get().artifactInfo().analyzed()).isBeforeOrEqualTo(LocalDateTime.now());
        assertArtifact(artifactTree.get().artifactInfo(), gav);
    }

    @Test
    void shouldCreateNewUnresolvedArtifact() {
        Gav gav = new Gav("x", "y", "1");
        ArtifactInfo artifact = ArtifactInfo.unresolved(gav, "test");
        repo.saveArtifact(artifact);
        Optional<ArtifactTree> artifactTree = repo.find(gav);

        assertThat(artifactTree).isPresent();
        assertThat(artifactTree.get().dependencies()).isEmpty();
        assertThat(artifactTree.get().artifactInfo().analyzed()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(artifactTree.get().artifactInfo().unresolved()).isTrue();
        assertThat(artifactTree.get().artifactInfo().unresolvedCount()).isEqualTo(1);
        assertThat(artifactTree.get().artifactInfo().unresolvedReason()).isEqualTo("test");
    }

    @Test
    void shouldOverwriteUnresolvedReasonAndCount() {
        Gav gav = new Gav("x", "y", "1");
        repo.saveArtifact(ArtifactInfo.unresolved(gav, "test1"));
        repo.saveArtifact(ArtifactInfo.unresolved(gav, "test2"));
        Optional<ArtifactTree> artifactTree = repo.find(gav);

        assertThat(artifactTree).isPresent();
        assertThat(artifactTree.get().dependencies()).isEmpty();
        assertThat(artifactTree.get().artifactInfo().analyzed()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(artifactTree.get().artifactInfo().unresolved()).isTrue();
        assertThat(artifactTree.get().artifactInfo().unresolvedCount()).isEqualTo(2);
        assertThat(artifactTree.get().artifactInfo().unresolvedReason()).isEqualTo("test2");

        repo.saveArtifact(ArtifactInfo.unresolved(gav, "test3"));
        artifactTree = repo.find(gav);

        assertThat(artifactTree).isPresent();
        assertThat(artifactTree.get().dependencies()).isEmpty();
        assertThat(artifactTree.get().artifactInfo().analyzed()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(artifactTree.get().artifactInfo().unresolved()).isTrue();
        assertThat(artifactTree.get().artifactInfo().unresolvedCount()).isEqualTo(3);
        assertThat(artifactTree.get().artifactInfo().unresolvedReason()).isEqualTo("test3");
    }

    private static ArtifactInfo artifactInfo(Gav gav) {
        return new ArtifactInfo(gav.groupId(), gav.artifactId(), gav.version(), gav.classifier(), null, null, null,
                LocalDateTime.MIN, 10L, "52.0", "jar", "name", "desc", "url", "scmUrl",
                "issuesUrl", "1995", List.of(new License("MIT", "https://mit.com")), List.of(LicenseType.MIT), List.of("source"),
                new ArtifactInfo.EffectiveValues(0, 0, 0, 10L, "52.0", LicenseType.MIT, List.of()),
                null);
    }

    private void assertArtifact(ArtifactInfo info, Gav gav) {
        assertThat(info.groupId()).isEqualTo(gav.groupId());
        assertThat(info.artifactId()).isEqualTo(gav.artifactId());
        assertThat(info.version()).isEqualTo(gav.version());
        assertThat(info.packageSize()).isEqualTo(10L);
        assertThat(info.classifiers()).isEqualTo(List.of("source"));
    }
}