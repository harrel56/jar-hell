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
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
class PackageAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(PackageAnalyzer.class);
    private static final List<String> RANGE_STEPS = List.of("8096", "16384", "131072", "524288");

    private final HttpClient httpClient;

    PackageAnalyzer(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    PackageInfo analyzePackage(Gav gav, FilesInfo filesInfo, String packaging) {
        try {
            return fetchPackage(gav, filesInfo, packaging);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalArgumentException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException(e);
        }
    }

    private PackageInfo fetchPackage(Gav gav, FilesInfo filesInfo, String packaging) throws InterruptedException, ExecutionException, TimeoutException {
        if (filesInfo.extensions().contains("jar")) {
            return fetchJar(gav);
        } else {
            return fetchOther(gav, packaging);
        }
    }

    private PackageInfo fetchJar(Gav gav) throws InterruptedException, ExecutionException, TimeoutException {
        String url = MavenApiClient.createFileUrl(gav, "jar");
        LocalDateTime created = null;
        Long packageSize = null;
        for (String rangeStep : RANGE_STEPS) {
            InputStreamResponseListener listener = new InputStreamResponseListener();
            httpClient.newRequest(url)
                    .headers(headers -> headers.add("Range", "bytes=0-" + rangeStep))
                    .send(listener);
            Response res = listener.get(5L, TimeUnit.SECONDS);
            if (res.getStatus() >= 400) {
                throw new IllegalArgumentException("HTTP call failed [%s] for url [%s]".formatted(res.getStatus(), url));
            }

            if (created == null) {
                String lastModifiedHeader = Objects.requireNonNull(res.getHeaders().get("Last-Modified"));
                created = LocalDateTime.parse(lastModifiedHeader, DateTimeFormatter.RFC_1123_DATE_TIME);
            }
            if (packageSize == null) {
                Pattern rangeRegex = Pattern.compile("(\\d*$)");
                packageSize = Optional.ofNullable(res.getHeaders().get("Content-Range"))
                        .map(rangeRegex::matcher)
                        .filter(Matcher::find)
                        .map(Matcher::group)
                        .or(() -> Optional.ofNullable(res.getHeaders().get("Content-Length")))
                        .map(Long::valueOf)
                        .orElseThrow();
            }
            try {
                String byteCodeVersion = parseByteCodeVersion(listener.getInputStream());
                return new PackageInfo(created, packageSize, byteCodeVersion);
            } catch (IOException e) {
                if (packageSize < Long.parseLong(rangeStep)) {
                    break;
                }
                logger.info("Parsing jar failed for [{}] and range [{}]. Retrying with bigger range...", gav, rangeStep);
            }
        }
        logger.info("No class files found in jar [{}]. Assuming no bytecode", gav);
        return new PackageInfo(created, packageSize, null);
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

    private String parseByteCodeVersion(InputStream is) throws IOException {
        JarInputStream jis = new JarInputStream(is);
        JarEntry entry = jis.getNextJarEntry();
        while (entry != null && !(entry.getName().endsWith(".class") && !entry.getName().equals("module-info.class"))) {
            entry = jis.getNextJarEntry();
        }

        DataInputStream dis = new DataInputStream(jis);
        int magicNumber = dis.readInt();
        if (magicNumber != 0xCAFEBABE) {
            throw new IllegalArgumentException("Invalid magic number");
        }
        int minor = 0xFFFF & dis.readShort();
        int major = 0xFFFF & dis.readShort();
        return major + "." + minor;
    }
}
