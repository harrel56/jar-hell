package dev.harrel.jarhell;

import dev.harrel.jarhell.model.Gav;
import io.avaje.config.Config;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.StructuredTaskScope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MavenApiClientTest {
    private static final String CONTENT_URL = Config.get("maven.repo-url");
    private static final URI DIR_URL = URI.create(CONTENT_URL + "/dev%2Fharrel%2Foops.hello");
    private static final URI METADATA_URL = URI.create(CONTENT_URL + "/dev%2Fharrel%2Foops.hello%2Fmaven-metadata.xml");

    private CustomHttpClient httpClient;
    private MavenApiClient mavenApiClient;

    @BeforeEach
    void setUp() {
        this.httpClient = mock(CustomHttpClient.class);
        this.mavenApiClient = new MavenApiClient(new Configuration().objectMapper(), httpClient);
    }

    @Test
    void artifactExistsFor200() throws Exception {
        when(httpClient.sendGet(any(), anyLong())).thenReturn(new ContentResponseMock(200, null));
        boolean res = mavenApiClient.checkIfArtifactExists(new Gav("a", "b", "1.0.0"));

        assertThat(res).isTrue();
    }


    @ParameterizedTest
    @ValueSource(ints = {201, 300, 400, 401, 403, 404, 500, 501, 502, 503})
    void artifactDoesNotExistForOtherStatus(int status) throws Exception {
        when(httpClient.sendGet(any(), anyLong())).thenReturn(new ContentResponseMock(status, null));
        boolean res = mavenApiClient.checkIfArtifactExists(new Gav("a", "b", "1.0.0"));

        assertThat(res).isFalse();
    }

    @Test
    void mergesVersionsFromDirAndMetadata() throws Exception {
        ContentResponseMock dirHttpRes = new ContentResponseMock(200, """
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
        ContentResponseMock metadataHttpRes = new ContentResponseMock(200, """
                <version>1.0.0</version>
                <version>1.5.1</version>""");
        when(httpClient.sendGet(eq(DIR_URL), anyLong())).thenReturn(dirHttpRes);
        when(httpClient.sendGet(eq(METADATA_URL), anyLong())).thenReturn(metadataHttpRes);
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
        ContentResponseMock httpRes = new ContentResponseMock(200, "what?");
        when(httpClient.sendGet(any(), anyLong())).thenReturn(httpRes);

        assertThatThrownBy(() -> mavenApiClient.fetchArtifactVersions("dev.harrel", "oops.hello"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ignoresDirVersionsIf404() throws Exception {
        ContentResponseMock httpRes = new ContentResponseMock(404, "what?");
        ContentResponseMock metadataHttpRes = new ContentResponseMock(200, """
                <version>1.0.0</version>
                <version>1.5.1</version>""");
        when(httpClient.sendGet(eq(DIR_URL), anyLong())).thenReturn(httpRes);
        when(httpClient.sendGet(eq(METADATA_URL), anyLong())).thenReturn(metadataHttpRes);

        List<String> res = mavenApiClient.fetchArtifactVersions("dev.harrel", "oops.hello");

        assertThat(res).containsExactly("1.0.0", "1.5.1");
    }

    @Test
    void ignoresMetadataVersionsIf404() throws Exception {
        ContentResponseMock httpRes = new ContentResponseMock(200, "<a href=\"1.0.0/\"></a>");
        ContentResponseMock metadataHttpRes = new ContentResponseMock(404, "error");
        when(httpClient.sendGet(eq(DIR_URL), anyLong())).thenReturn(httpRes);
        when(httpClient.sendGet(eq(METADATA_URL), anyLong())).thenReturn(metadataHttpRes);

        List<String> res = mavenApiClient.fetchArtifactVersions("dev.harrel", "oops.hello");

        assertThat(res).containsExactly("1.0.0");
    }

    @Test
    void failsIfDirVersionsFail() throws Exception {
        ContentResponseMock metadataHttpRes = new ContentResponseMock(200, "");
        IllegalArgumentException iae = new IllegalArgumentException();
        when(httpClient.sendGet(eq(DIR_URL), anyLong())).thenThrow(iae);
        when(httpClient.sendGet(eq(METADATA_URL), anyLong())).thenReturn(metadataHttpRes);

        assertThatThrownBy(() -> mavenApiClient.fetchArtifactVersions("dev.harrel", "oops.hello"))
                .isInstanceOf(StructuredTaskScope.FailedException.class)
                .hasCause(iae);
    }

    @Test
    void failsIfMetadataVersionsFail() throws Exception {
        ContentResponseMock dirHttpRes = new ContentResponseMock(200, "");
        IllegalArgumentException iae = new IllegalArgumentException();
        when(httpClient.sendGet(eq(DIR_URL), anyLong())).thenReturn(dirHttpRes);
        when(httpClient.sendGet(eq(METADATA_URL), anyLong())).thenThrow(iae);

        assertThatThrownBy(() -> mavenApiClient.fetchArtifactVersions("dev.harrel", "oops.hello"))
                .isInstanceOf(StructuredTaskScope.FailedException.class)
                .hasCause(iae);
    }

    public static class ContentResponseMock implements ContentResponse {
        private final int statusCode;
        private final String body;

        public ContentResponseMock(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        @Override
        public int getStatus() {
            return statusCode;
        }

        @Override
        public byte[] getContent() {
            return body.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public String getContentAsString() {
            return body;
        }

        @Override
        public String getMediaType() {
            return "";
        }

        @Override
        public String getEncoding() {
            return "";
        }

        @Override
        public Request getRequest() {
            return null;
        }

        @Override
        public <T extends ResponseListener> List<T> getListeners(Class<T> listenerClass) {
            return List.of();
        }

        @Override
        public HttpVersion getVersion() {
            return null;
        }

        @Override
        public String getReason() {
            return "";
        }

        @Override
        public HttpFields getHeaders() {
            return null;
        }

        @Override
        public boolean abort(Throwable cause) {
            return false;
        }
    }
}