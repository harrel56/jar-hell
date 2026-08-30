package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.MavenApiClient;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.model.PackageInfo;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.jar.JarInputStream;

@Singleton
class PackageAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(PackageAnalyzer.class);

    private final HttpClient httpClient;
    private final JarAnalyzer jarAnalyzer = new JarAnalyzer();

    PackageAnalyzer(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    PackageInfo analyzePackage(Gav gav, FilesInfo filesInfo, String packaging) {
        try {
            if (filesInfo.extensions().contains("jar")) {
                return fetchJar(gav);
            } else {
                return fetchOther(gav, packaging);
            }
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalArgumentException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException(e);
        }
    }

    private PackageInfo fetchJar(Gav gav) throws InterruptedException, ExecutionException, TimeoutException {
        String url = MavenApiClient.createFileUrl(gav, "jar");
        InputStreamResponseListener listener = new InputStreamResponseListener();
        httpClient.newRequest(url).send(listener);
        Response res = listener.get(5L, TimeUnit.SECONDS);
        if (res.getStatus() >= 400) {
            throw new IllegalArgumentException("HTTP call failed [%s] for url [%s]".formatted(res.getStatus(), url));
        }

        String lastModifiedHeader = Objects.requireNonNull(res.getHeaders().get("Last-Modified"));
        LocalDateTime created = LocalDateTime.parse(lastModifiedHeader, DateTimeFormatter.RFC_1123_DATE_TIME);
        Long packageSize = Long.valueOf(res.getHeaders().get("Content-Length"));

        try {
            JarAnalyzer.JarInfo jarInfo = jarAnalyzer.analyzeJar(new JarInputStream(listener.getInputStream()));
            return new PackageInfo(created, packageSize, jarInfo);
        } catch (IOException e) {
            logger.info("Analyzing jar failed for [{}]", gav, e);
            return new PackageInfo(created, packageSize, null);
        }
    }

    private PackageInfo fetchOther(Gav gav, String packaging) throws InterruptedException, ExecutionException, TimeoutException {
        String url = MavenApiClient.createFileUrl(gav, packaging);
        InputStreamResponseListener listener = new InputStreamResponseListener();
        httpClient.newRequest(url)
                .method(HttpMethod.HEAD)
                .send(listener);
        Response res = listener.get(5L, TimeUnit.SECONDS);
        if (res.getStatus() >= 400) {
            throw new IllegalArgumentException("HTTP call failed [%s] for url [%s]".formatted(res.getStatus(), url));
        }

        String lastModifiedHeader = Objects.requireNonNull(res.getHeaders().get("Last-Modified"));
        LocalDateTime created = LocalDateTime.parse(lastModifiedHeader, DateTimeFormatter.RFC_1123_DATE_TIME);

        String contentLengthHeader = Objects.requireNonNull(res.getHeaders().get("Content-Length"));
        long packageSize = Long.parseLong(contentLengthHeader);

        return new PackageInfo(created, packageSize, null);
    }
}
