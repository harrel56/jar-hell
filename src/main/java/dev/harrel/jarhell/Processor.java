package dev.harrel.jarhell;

import io.nats.jparse.Json;
import io.nats.jparse.Path;
import io.nats.jparse.node.RootNode;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class Processor {

    private final HttpClient httpClient;
    private final PomProcessor pomProcessor;

    Processor() throws ParserConfigurationException {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        this.pomProcessor = new PomProcessor(httpClient);
    }

    void process(String group, String artifact) throws IOException, InterruptedException, XPathExpressionException, ParserConfigurationException, SAXException {
        String version = fetchLatestVersion(group, artifact);
        ArtifactCoordinate cord = new ArtifactCoordinate(group, artifact, version);
        System.out.println(version);

        JarInfo jarInfo = fetchJarInfo(cord);
        System.out.println(jarInfo);

        PomInfo pomInfo = pomProcessor.processPom(cord);
        System.out.println(pomInfo);
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

    private JarInfo fetchJarInfo(ArtifactCoordinate cord) throws IOException, InterruptedException {
        String groupPath = cord.group().replace('.', '/');
        String fileName = "%s-%s.jar".formatted(cord.id(), cord.version());
        String query = "?filepath=%s/%s/%s/%s".formatted(groupPath, cord.id(), cord.version(), fileName);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://search.maven.org/remotecontent" + query))
                .header("Range", "bytes=0-1024")
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
        System.out.println("jar size: " + jarSize);
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

record ArtifactCoordinate(String group, String id, String version) {}
record JarInfo(long size, String bytecodeVersion) {}
