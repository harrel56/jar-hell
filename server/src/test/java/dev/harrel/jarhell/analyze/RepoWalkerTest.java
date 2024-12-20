package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.MavenApiClientTest;
import dev.harrel.jarhell.extension.EnvironmentTest;
import org.eclipse.jetty.client.HttpClient;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void doesntFailForFailedRequests() throws ExecutionException, InterruptedException, TimeoutException {
        HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.GET((URI) any())).thenReturn(new MavenApiClientTest.ContentResponseMock(200, """
                <a href="../">../</a>
                <a href="path/">path/</a>
                """));
        when(httpClient.GET(argThat(uriEndsWith("/path/")))).thenReturn(new MavenApiClientTest.ContentResponseMock(404, "err"));

        new RepoWalker(httpClient).walk(_ -> {});
    }

    private static ArgumentMatcher<URI> uriEndsWith(String suffix) {
        return uri -> uri != null && uri.toString().endsWith(suffix);
    }
}