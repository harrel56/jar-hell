package dev.harrel.jarhell;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpClientTransport;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.FutureResponseListener;

import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CustomHttpClient extends HttpClient {
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
}
