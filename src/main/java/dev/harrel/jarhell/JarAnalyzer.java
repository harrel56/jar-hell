package dev.harrel.jarhell;

import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.model.JarInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JarAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(JarAnalyzer.class);
    private static final List<String> RANGE_STEPS = List.of("8096", "16384", "131072", "524288");

    private final HttpClient httpClient;

    public JarAnalyzer(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public JarInfo analyzeJar(Gav gav) throws InterruptedException, IOException {
        String groupPath = gav.groupId().replace('.', '/');
        String fileName = "%s-%s.jar".formatted(gav.artifactId(), gav.version());
        String query = "?filepath=%s/%s/%s/%s".formatted(groupPath, gav.artifactId(), gav.version(), fileName);

        long jarSize = -1L;
        for (String rangeStep : RANGE_STEPS) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://search.maven.org/remotecontent" + query))
                    .header("Range", "bytes=0-" + rangeStep)
                    .GET()
                    .build();
            // todo: handle HTTP responses
            HttpResponse<InputStream> streamResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            Pattern rangeRegex = Pattern.compile("(\\d*$)");
            jarSize = streamResponse.headers()
                    .firstValue("Content-Range")
                    .map(rangeRegex::matcher)
                    .filter(Matcher::find)
                    .map(Matcher::group)
                    .or(() -> streamResponse.headers().firstValue("Content-Length"))
                    .map(Long::valueOf)
                    .orElseThrow();
            try {
                String byteCodeVersion = parseByteCodeVersion(streamResponse);
                return new JarInfo(jarSize, byteCodeVersion);
            } catch (IOException e) {
                logger.warn("Parsing jar failed for [{}] and range [{}]. Retrying with bigger range...", gav, rangeStep);
            }
        }
        logger.warn("No class files found in jar [{}]. Assuming no bytecode", gav);
        return new JarInfo(jarSize, null);
    }

    private String parseByteCodeVersion(HttpResponse<InputStream> streamResponse) throws IOException {
        InputStream is = streamResponse.body();
        JarInputStream jis = new JarInputStream(is);
        JarEntry entry = jis.getNextJarEntry();
        while (entry != null && !entry.getName().endsWith(".class")) {
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
