package dev.harrel.jarhell;

import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.model.JarInfo;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JarAnalyzer {
    private final HttpClient httpClient;

    public JarAnalyzer(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public JarInfo analyzeJar(Gav gav) throws IOException, InterruptedException {
        String groupPath = gav.groupId().replace('.', '/');
        String fileName = "%s-%s.jar".formatted(gav.artifactId(), gav.version());
        String query = "?filepath=%s/%s/%s/%s".formatted(groupPath, gav.artifactId(), gav.version(), fileName);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://search.maven.org/remotecontent" + query))
                // todo: this range can always be too small - need to retry with bigger range if it fails
                .header("Range", "bytes=0-16384")
                .GET()
                .build();
        // todo: handle HTTP responses
        HttpResponse<InputStream> inputResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        Pattern rangeRegex = Pattern.compile("(\\d*$)");
        Long jarSize = inputResponse.headers()
                .firstValue("Content-Range")
                .map(rangeRegex::matcher)
                .filter(Matcher::find)
                .map(Matcher::group)
                .or(() -> inputResponse.headers().firstValue("Content-Length"))
                .map(Long::valueOf)
                .orElseThrow();
        InputStream is = inputResponse.body();
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
        return new JarInfo(jarSize, major + "." + minor);
    }
}
