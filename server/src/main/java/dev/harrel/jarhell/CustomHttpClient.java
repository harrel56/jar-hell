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
    public CustomHttpClient(HttpClientTransport transport) {
        super(transport);
    }

    public ContentResponse GET(String uri, int maxResponseSize) throws ExecutionException, InterruptedException, TimeoutException {
        return GET(URI.create(uri), maxResponseSize);
    }

    public ContentResponse GET(URI uri, int maxResponseSize) throws ExecutionException, InterruptedException, TimeoutException {
        Request req = newRequest(uri);
        FutureResponseListener listener = new FutureResponseListener(req, maxResponseSize);
        req.send(listener);
        return listener.get(5L, TimeUnit.SECONDS);
    }
}
