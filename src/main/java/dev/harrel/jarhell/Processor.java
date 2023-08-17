package dev.harrel.jarhell;

import dev.harrel.jarhell.model.ArtifactInfo;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.model.JarInfo;
import dev.harrel.jarhell.model.PomInfo;
import io.nats.jparse.Json;
import io.nats.jparse.Path;
import io.nats.jparse.node.RootNode;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
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

public class Processor {
    private final HttpClient httpClient;
    private final PomProcessor pomProcessor;
    private final DependencyResolver dependencyResolver;

    Processor() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        this.pomProcessor = new PomProcessor(httpClient);
        this.dependencyResolver = new DependencyResolver();
    }

    ArtifactInfo process(String group, String artifact) throws IOException, InterruptedException, XPathExpressionException, ParserConfigurationException, SAXException {
        String version = fetchLatestVersion(group, artifact);
        Gav gav = new Gav(group, artifact, version);
        JarInfo jarInfo = fetchJarInfo(gav);
        PomInfo pomInfo = pomProcessor.computePomInfo(gav);
        dependencyResolver.resolveDependencies(gav);

        return new ArtifactInfo(gav, jarInfo, pomInfo);
    }

    private String fetchLatestVersion(String group, String artifact) throws IOException, InterruptedException {
        String query = "?q=g:%s+AND+a:%s".formatted(group, artifact);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://search.maven.org/solrsearch/select" + query))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        RootNode rootNode = Json.toRootNode(response.body());
        return Path.atPath("response.docs[0].latestVersion", rootNode).toJsonString();
    }

    private JarInfo fetchJarInfo(Gav gav) throws IOException, InterruptedException {
        String groupPath = gav.groupId().replace('.', '/');
        String fileName = "%s-%s.jar".formatted(gav.artifactId(), gav.version());
        String query = "?filepath=%s/%s/%s/%s".formatted(groupPath, gav.artifactId(), gav.version(), fileName);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://search.maven.org/remotecontent" + query))
                .header("Range", "bytes=0-2048")
                .GET()
                .build();
        HttpResponse<InputStream> inputResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        Pattern rangeRegex = Pattern.compile("(\\d*$)");
        Long jarSize = inputResponse.headers()
                .firstValue("Content-Range")
                .map(rangeRegex::matcher)
                .filter(Matcher::find)
                .map(Matcher::group)
                .map(Long::valueOf)
                .orElseThrow();
        InputStream is = inputResponse.body();
        JarInputStream jis = new JarInputStream(is);
        JarEntry entry = jis.getNextJarEntry();
        while (entry != null && !entry.getName().endsWith(".class")) {
            jis.skip(entry.getSize());
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

