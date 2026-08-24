package dev.harrel.jarhell;

import dev.harrel.jarhell.model.Gav;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MavenApiClientTest {
    private CustomHttpClient httpClient;
    private MavenApiClient mavenApiClient;

    @BeforeEach
    void setUp() {
        this.httpClient = mock(CustomHttpClient.class);
        this.mavenApiClient = new MavenApiClient(httpClient);
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