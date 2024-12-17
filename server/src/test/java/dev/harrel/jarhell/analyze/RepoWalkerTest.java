package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.extension.EnvironmentTest;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

@EnvironmentTest
class RepoWalkerTest {
    @Test
    void collectsGavsCorrectly(RepoWalker repoWalker) {
        Set<RepoWalker.State> gavs = Collections.newSetFromMap(new ConcurrentHashMap<>());
        repoWalker.walk(gavs::add);
        assertThat(gavs).contains(
                new RepoWalker.State("org.test", "artifact", List.of("1.0.10", "1.1.0", "3.0.1")),
                new RepoWalker.State("org.test", "pre-cycle", List.of("1.0.0")),
                new RepoWalker.State("org.test", "cycle1", List.of("1.0.0")),
                new RepoWalker.State("org.test", "cycle2", List.of("1.0.0")),
                new RepoWalker.State("org.test", "cycle3", List.of("1.0.0")),
                new RepoWalker.State("com.sanctionco.jmail", "jmail", List.of("1.6.2")),
                new RepoWalker.State("dev.harrel", "json-schema", List.of("1.5.0"))
        );
    }
}