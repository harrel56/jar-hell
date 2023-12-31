package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.MavenApiClient;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.model.PackageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
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
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException(e);
        }
    }

    private PackageInfo fetchPackage(Gav gav, FilesInfo filesInfo, String packaging) throws InterruptedException, IOException {
        String packageExtension;
        if (filesInfo.extensions().contains("jar")) {
            packageExtension = "jar";
        } else if (filesInfo.extensions().contains(packaging)) {
            packageExtension = packaging;
        } else {
            logger.warn("Couldn't determine package extension for artifact: [{}], packaging: [{}], extensions: {}", gav, packaging, filesInfo.extensions());
            return new PackageInfo(null, null);
        }
        String url = MavenApiClient.createFileUrl(gav, packageExtension);
        Long packageSize = null;
        for (String rangeStep : RANGE_STEPS) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Range", "bytes=0-" + rangeStep)
                    .GET()
                    .build();
            HttpResponse<InputStream> streamResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (streamResponse.statusCode() >= 400) {
                throw new IllegalArgumentException("HTTP call failed [%s] for url [%s]".formatted(streamResponse.statusCode(), url));
            }

            Pattern rangeRegex = Pattern.compile("(\\d*$)");
            packageSize = streamResponse.headers()
                    .firstValue("Content-Range")
                    .map(rangeRegex::matcher)
                    .filter(Matcher::find)
                    .map(Matcher::group)
                    .or(() -> streamResponse.headers().firstValue("Content-Length"))
                    .map(Long::valueOf)
                    .orElseThrow();
            try {
                String byteCodeVersion = "jar".equals(packaging) ? parseByteCodeVersion(streamResponse) : null;
                return new PackageInfo(packageSize, byteCodeVersion);
            } catch (IOException e) {
                logger.info("Parsing jar failed for [{}] and range [{}]. Retrying with bigger range...", gav, rangeStep);
                if (packageSize < Long.parseLong(rangeStep)) {
                    break;
                }
            }
        }
        logger.info("No class files found in jar [{}]. Assuming no bytecode", gav);
        return new PackageInfo(packageSize, null);
    }

    private String parseByteCodeVersion(HttpResponse<InputStream> streamResponse) throws IOException {
        InputStream is = streamResponse.body();
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
