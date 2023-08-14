package dev.harrel.jarhell;

import io.nats.jparse.Json;
import io.nats.jparse.Path;
import io.nats.jparse.node.RootNode;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] arg) throws IOException, InterruptedException, DocumentException {
        String group = "dev.harrel";
        String artifact = "json-schema";
        String query = "?q=g:%s+AND+a:%s".formatted(group, artifact);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://search.maven.org/solrsearch/select" + query))
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newBuilder()
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());

        RootNode rootNode = Json.toRootNode(response.body());
        String latestVersion = Path.atPath("response.docs[0].latestVersion", rootNode).toJsonString();
        System.out.println(latestVersion);

        String groupPath = group.replace('.', '/');
        String fileName = "%s-%s.jar".formatted(artifact, latestVersion);
        query = "?filepath=%s/%s/%s/%s".formatted(groupPath, artifact, latestVersion, fileName);
        request = HttpRequest.newBuilder()
                .uri(URI.create("https://search.maven.org/remotecontent" + query))
                .header("Range", "bytes=0-1024")
                .GET()
                .build();
        HttpResponse<InputStream> inputResponse = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build()
                .send(request, HttpResponse.BodyHandlers.ofInputStream());

        Pattern rangeRegex = Pattern.compile("(\\d*$)");
        String jarSize = inputResponse.headers()
                .firstValue("Content-Range")
                .map(rangeRegex::matcher)
                .filter(Matcher::find)
                .map(Matcher::group)
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
        System.out.println("bytecode version: " + major + "." + minor);
        SAXReader saxReader = new SAXReader();
        Document document = saxReader.read(new StringReader("<hello>witam</hello>"));
        System.out.println(document.selectNodes("//hello").get(0).getText());

    }
}
