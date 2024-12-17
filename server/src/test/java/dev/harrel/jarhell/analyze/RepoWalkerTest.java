package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.extension.EnvironmentTest;
import dev.harrel.jarhell.model.Gav;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

@EnvironmentTest
class RepoWalkerTest {
    @Test
    void collectsGavsCorrectly(RepoWalker repoWalker) {
        Set<Gav> gavs = Collections.newSetFromMap(new ConcurrentHashMap<>());
        repoWalker.walk(state -> state.versions().forEach(v -> gavs.add(new Gav(state.groupId(), state.artifactId(), v))));
        assertThat(gavs).contains(
                new Gav("org.test", "artifact", "1.0.10"),
                new Gav("org.test", "artifact", "1.1.0"),
                new Gav("org.test", "artifact", "3.0.1"),
                new Gav("org.test", "pre-cycle", "1.0.0"),
                new Gav("org.test", "cycle1", "1.0.0"),
                new Gav("org.test", "cycle2", "1.0.0"),
                new Gav("org.test", "cycle3", "1.0.0"),
                new Gav("org.test", "cycle3", "1.0.0"),
                new Gav("com.sanctionco.jmail", "jmail", "1.6.2")
        );
    }
}