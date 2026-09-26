package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.MavenApiClient;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.model.PackageInfo;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.InputStreamResponseListener;
import org.eclipse.jetty.client.Response;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Set;
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

    PackageInfo analyzePackage(Gav gav, FilesInfo filesInfo) {
        try {
            String extension = selectExtension(filesInfo.extensions());
            if ("jar".equals(extension)) {
                return fetchJar(gav);
            } else {
                return fetchOther(gav, extension);
            }
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalArgumentException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException(e);
        }
    }

    static String selectExtension(Set<String> extensions) {
        if (extensions.contains("jar")) {
            return "jar";
        }
        return extensions.stream()
                .filter(ext -> !ext.equals("pom") && !ext.equals("module") && !MavenIndexService.CHECKSUM_EXTENSIONS.contains(ext))
                .sorted()
                .findFirst()
                .or(() -> extensions.stream().filter("pom"::equals).findFirst())
                .orElseThrow(() -> new IllegalArgumentException("No artifact files found among: " + extensions));
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

    private PackageInfo fetchOther(Gav gav, String extension) throws InterruptedException, ExecutionException, TimeoutException {
        String url = MavenApiClient.createFileUrl(gav, extension);
        InputStreamResponseListener listener = new InputStreamResponseListener();
        // Central gzips text formats (json, xml, pom) on the fly and then omits Content-Length,
        // so opt out of the gzip that Jetty advertises by default to get the real file size
        httpClient.newRequest(url)
                .method(HttpMethod.HEAD)
                .headers(headers -> headers.put(HttpHeader.ACCEPT_ENCODING, "identity"))
                .send(listener);
        Response res = listener.get(5L, TimeUnit.SECONDS);
        if (res.getStatus() >= 400) {
            throw new IllegalArgumentException("HTTP call failed [%s] for url [%s]".formatted(res.getStatus(), url));
        }

        String lastModifiedHeader = Objects.requireNonNull(res.getHeaders().get("Last-Modified"));
        LocalDateTime created = LocalDateTime.parse(lastModifiedHeader, DateTimeFormatter.RFC_1123_DATE_TIME);

        long packageSize = 0;
        if (!"pom".equals(extension)) {
            String contentLengthHeader = Objects.requireNonNull(res.getHeaders().get("Content-Length"));
            packageSize = Long.parseLong(contentLengthHeader);
        }

        return new PackageInfo(created, packageSize, null);
    }
}
