package dev.harrel.jarhell;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpClientTransport;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.FutureResponseListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CustomHttpClient extends HttpClient {
    private static final Logger logger = LoggerFactory.getLogger(CustomHttpClient.class);

    private static final int MAX_RESPONSE_SIZE = 16 * 1024 * 1024;

    public CustomHttpClient(HttpClientTransport transport) {
        super(transport);
    }

    public ContentResponse sendGet(URI uri, long timeout) throws ExecutionException, InterruptedException, TimeoutException {
        Request req = newRequest(uri);
        FutureResponseListener listener = new FutureResponseListener(req, MAX_RESPONSE_SIZE);
        req.send(listener);
        return listener.get(timeout, TimeUnit.SECONDS);
    }

    public ContentResponse sendGetWithRetries(URI uri, int retries) throws InterruptedException {
        for (int i = 0; i <= retries; i++) {
            try {
                ContentResponse res = sendGet(uri, 5L);
                if (res.getStatus() == 429 || res.getStatus() >= 500) {
                    throw new IllegalArgumentException("Responded with status [%d]".formatted(res.getStatus()));
                }
                return res;
            } catch (ExecutionException | TimeoutException | RuntimeException e) {
                logger.info("HTTP call failed for url [{}]. Try {} of {}. {}", uri, i, retries, e.toString());
                i++;
            }
        }
        throw new IllegalArgumentException("All tries [%d] failed".formatted(retries));
    }
}
