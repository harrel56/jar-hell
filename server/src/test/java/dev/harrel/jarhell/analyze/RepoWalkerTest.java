package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.CustomHttpClient;
import dev.harrel.jarhell.MavenApiClientTest;
import dev.harrel.jarhell.extension.EnvironmentTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnvironmentTest
class RepoWalkerTest {
    private final String repoUrl = "http://localhost:8181/snapshots";

    @Test
    void collectsGavsCorrectly(RepoWalker repoWalker) {
        Set<RepoWalker.ArtifactData> gavs = Collections.newSetFromMap(new ConcurrentHashMap<>());
        repoWalker.walk(repoUrl, gavs::add).join();

        assertThat(gavs).contains(
                new RepoWalker.ArtifactData("org.test", "artifact", List.of("1.0.10", "1.1.0", "3.0.1", "3.2.1")),
                new RepoWalker.ArtifactData("org.test", "pre-cycle", List.of("1.0.0")),
                new RepoWalker.ArtifactData("org.test", "cycle1", List.of("1.0.0")),
                new RepoWalker.ArtifactData("org.test", "cycle2", List.of("1.0.0")),
                new RepoWalker.ArtifactData("org.test", "cycle3", List.of("1.0.0")),
                new RepoWalker.ArtifactData("com.sanctionco.jmail", "jmail", List.of("1.6.2")),
                new RepoWalker.ArtifactData("dev.harrel", "json-schema", List.of("1.5.0"))
        );
    }

    @Test
    void doesntFailForFailedRequests() throws ExecutionException, InterruptedException, TimeoutException {
        CustomHttpClient httpClient = mock(CustomHttpClient.class);
        when(httpClient.sendGetWithRetries((URI) any(), anyInt())).thenReturn(new MavenApiClientTest.ContentResponseMock(200, """
                <a href="../">../</a>
                <a href="path/">path/</a>
                """));
        when(httpClient.sendGetWithRetries(argThat(uriEndsWith("/path/")), anyInt())).thenReturn(new MavenApiClientTest.ContentResponseMock(404, "err"));

        new RepoWalker(httpClient).walk(repoUrl, _ -> {}).get();
    }

    private static ArgumentMatcher<URI> uriEndsWith(String suffix) {
        return uri -> uri != null && uri.toString().endsWith(suffix);
    }
}