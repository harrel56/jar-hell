package dev.harrel.jarhell;

import dev.harrel.jarhell.model.Gav;
import io.avaje.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MavenApiClientTest {
    private static final String CONTENT_URL = Config.get("maven.repo-url");

    private HttpClient httpClient;
    private MavenApiClient mavenApiClient;

    @BeforeEach
    void setUp() {
        this.httpClient = mock(HttpClient.class);
        this.mavenApiClient = new MavenApiClient(new Configuration().objectMapper(), httpClient);
    }

    @Test
    void artifactExistsFor200() throws Exception {
        when(httpClient.send(any(), any())).thenReturn(new HttpResponseMock<>(200, null));
        boolean res = mavenApiClient.checkIfArtifactExists(new Gav("a", "b", "1.0.0"));

        assertThat(res).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {201, 300, 400, 401, 403, 404, 500, 501, 502, 503})
    void artifactDoesNotExistForOtherStatus(int status) throws Exception {
        when(httpClient.send(any(), any())).thenReturn(new HttpResponseMock<>(status, null));
        boolean res = mavenApiClient.checkIfArtifactExists(new Gav("a", "b", "1.0.0"));

        assertThat(res).isFalse();
    }

    @Test
    void mergesVersionsFromDirAndMetadata() throws Exception {
        HttpResponseMock<Object> dirHttpRes = new HttpResponseMock<>(200, """
                <a href="../">../</a>
                <a href="1.0.0/" title="1.0.0/">1.0.0/</a>
                <a href="1.0.0-beta.1/" title="1.0.0-beta.1/">1.0.0-beta.1/</a>
                <a href="1.0.0-beta.2/" title="1.0.0-beta.2/">1.0.0-beta.2/</a>
                <a href="1.0.0-beta.3/" title="1.0.0-beta.3/">1.0.0-beta.3/</a>
                <a href="1.0.0-beta.4/" title="1.0.0-beta.4/">1.0.0-beta.4/</a>
                <a href="1.1.0/" title="1.1.0/">1.1.0/</a>
                <a href="1.2.0/" title="1.2.0/">1.2.0/</a>
                <a href="1.2.1/" title="1.2.1/">1.2.1/</a>
                <a href="1.2.2/" title="1.2.2/">1.2.2/</a>
                <a href="1.3.0/" title="1.3.0/">1.3.0/</a>
                <a href="1.3.1/" title="1.3.1/">1.3.1/</a>
                <a href="1.3.2/" title="1.3.2/">1.3.2/</a>
                <a href="1.3.3/" title="1.3.3/">1.3.3/</a>
                <a href="1.3.4/" title="1.3.4/">1.3.4/</a>
                <a href="1.4.0/" title="1.4.0/">1.4.0/</a>
                <a href="1.4.1/" title="1.4.1/">1.4.1/</a>
                <a href="1.4.2/" title="1.4.2/">1.4.2/</a>
                <a href="1.4.3/" title="1.4.3/">1.4.3/</a>
                <a href="1.4.4" title="1.4.4/">1.4.4/</a>
                <a href="this-is-not-version/" title="this-is-not-version/">this-is-not-version/</a>
                <a href="1.5.0/" title="1.5.0/">1.5.0/</a>""");
        HttpResponseMock<Object> metadataHttpRes = new HttpResponseMock<>(200, """
                <version>1.0.0</version>
                <version>1.5.1</version>""");
        when(httpClient.send(eq(dirRequest()), any())).thenReturn(dirHttpRes);
        when(httpClient.send(eq(metadataRequest()), any())).thenReturn(metadataHttpRes);
        List<String> res = mavenApiClient.fetchArtifactVersions("dev.harrel", "oops.hello");

        assertThat(res).containsExactly(
                "1.0.0-beta.1",
                "1.0.0-beta.2",
                "1.0.0-beta.3",
                "1.0.0-beta.4",
                "1.0.0",
                "1.1.0",
                "1.2.0",
                "1.2.1",
                "1.2.2",
                "1.3.0",
                "1.3.1",
                "1.3.2",
                "1.3.3",
                "1.3.4",
                "1.4.0",
                "1.4.1",
                "1.4.2",
                "1.4.3",
                "1.5.0",
                "1.5.1"
        );
    }

    @Test
    void failsIfDirAndMetadataVersionsAreEmpty() throws Exception {
        HttpResponseMock<Object> httpRes = new HttpResponseMock<>(200, "what?");
        when(httpClient.send(any(), any())).thenReturn(httpRes);

        assertThatThrownBy(() -> mavenApiClient.fetchArtifactVersions("dev.harrel", "oops.hello"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ignoresDirVersionsIf404() throws Exception {
        HttpResponseMock<Object> httpRes = new HttpResponseMock<>(404, "what?");
        HttpResponseMock<Object> metadataHttpRes = new HttpResponseMock<>(200, """
                <version>1.0.0</version>
                <version>1.5.1</version>""");
        when(httpClient.send(eq(dirRequest()), any())).thenReturn(httpRes);
        when(httpClient.send(eq(metadataRequest()), any())).thenReturn(metadataHttpRes);

        List<String> res = mavenApiClient.fetchArtifactVersions("dev.harrel", "oops.hello");

        assertThat(res).containsExactly("1.0.0", "1.5.1");
    }

    @Test
    void ignoresMetadataVersionsIf404() throws Exception {
        HttpResponseMock<Object> httpRes = new HttpResponseMock<>(200, "<a href=\"1.0.0/\"></a>");
        HttpResponseMock<Object> metadataHttpRes = new HttpResponseMock<>(404, "error");
        when(httpClient.send(eq(dirRequest()), any())).thenReturn(httpRes);
        when(httpClient.send(eq(metadataRequest()), any())).thenReturn(metadataHttpRes);

        List<String> res = mavenApiClient.fetchArtifactVersions("dev.harrel", "oops.hello");

        assertThat(res).containsExactly("1.0.0");
    }

    @Test
    void failsIfDirVersionsFail() throws Exception {
        HttpResponseMock<Object> metadataHttpRes = new HttpResponseMock<>(200, "");
        IllegalArgumentException iae = new IllegalArgumentException();
        when(httpClient.send(eq(dirRequest()), any())).thenThrow(iae);
        when(httpClient.send(eq(metadataRequest()), any())).thenReturn(metadataHttpRes);

        assertThatThrownBy(() -> mavenApiClient.fetchArtifactVersions("dev.harrel", "oops.hello"))
                .isInstanceOf(CompletionException.class)
                .hasCause(iae);
    }

    @Test
    void failsIfMetadataVersionsFail() throws Exception {
        HttpResponseMock<Object> dirHttpRes = new HttpResponseMock<>(200, "");
        IllegalArgumentException iae = new IllegalArgumentException();
        when(httpClient.send(eq(dirRequest()), any())).thenReturn(dirHttpRes);
        when(httpClient.send(eq(metadataRequest()), any())).thenThrow(iae);

        assertThatThrownBy(() -> mavenApiClient.fetchArtifactVersions("dev.harrel", "oops.hello"))
                .isInstanceOf(CompletionException.class)
                .hasCause(iae);
    }

    private static HttpRequest dirRequest() {
        return HttpRequest.newBuilder()
                .uri(URI.create(CONTENT_URL + "/dev%2Fharrel%2Foops.hello"))
                .GET()
                .build();
    }

    private static HttpRequest metadataRequest() {
        return HttpRequest.newBuilder()
                .uri(URI.create(CONTENT_URL + "/dev%2Fharrel%2Foops.hello%2Fmaven-metadata.xml"))
                .GET()
                .build();
    }

    private static class HttpResponseMock<T> implements HttpResponse<T> {
        private final int statusCode;
        private final T body;

        HttpResponseMock(int statusCode, T body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        @Override
        public int statusCode() {
            return statusCode;
        }

        @Override
        public HttpRequest request() {
            return null;
        }

        @Override
        public T body() {
            return body;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return null;
        }

        @Override
        public Optional<HttpResponse<T>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return null;
        }

        @Override
        public HttpClient.Version version() {
            return null;
        }
    }
}