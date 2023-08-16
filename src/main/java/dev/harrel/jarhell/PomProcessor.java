package dev.harrel.jarhell;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public class PomProcessor {
    private final HttpClient httpClient;
    private final DocumentBuilderFactory documentBuilderFactory;
    private final XPathFactory xPathFactory;

    public PomProcessor(HttpClient httpClient) throws ParserConfigurationException {
        this.httpClient = httpClient;
        this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
        this.documentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        this.documentBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        this.documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        this.documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        this.xPathFactory = XPathFactory.newInstance();
    }

    PomInfo processPom(Gav cord) throws IOException, InterruptedException, ParserConfigurationException, SAXException, XPathExpressionException {
        String groupPath = cord.group().replace('.', '/');
        String fileName = "%s-%s.pom".formatted(cord.id(), cord.version());
        String query = "?filepath=%s/%s/%s/%s".formatted(groupPath, cord.id(), cord.version(), fileName);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://search.maven.org/remotecontent" + query))
                .GET()
                .build();
        HttpResponse<InputStream> inputResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
        Document document = builder.parse(new BufferedInputStream(inputResponse.body()));
        XPath xPath = xPathFactory.newXPath();
        String packaging = xPathString(xPath, document, "//project/packaging").orElse(null);
        NodeList nodeList = (NodeList) xPath.compile("//project/dependencies/dependency").evaluate(document, XPathConstants.NODESET);
        List<ArtifactDependency> deps = IntStream.range(0, nodeList.getLength())
                .mapToObj(nodeList::item)
                .map(node -> nodeToDependency(xPath, node))
                .toList();

        return new PomInfo(packaging, deps);
    }

    private ArtifactDependency nodeToDependency(XPath xPath, Node node) {
        String groupId = xPathString(xPath, node, "groupId").orElseThrow();
        String artifactId = xPathString(xPath, node, "artifactId").orElseThrow();
        String version = xPathString(xPath, node, "version").orElseThrow();
        String scope = xPathString(xPath, node, "scope").orElse("compile");
        boolean optional = xPathString(xPath, node, "optional")
                .map(res -> res.trim().equals("true"))
                .orElse(false);
        return new ArtifactDependency(new Gav(groupId, artifactId, version), scope, optional);
    }

    private Optional<String> xPathString(XPath xPath, Node node, String query) {
        try {
            String result = xPath.compile(query).evaluate(node);
            return result.isBlank() ? Optional.empty() : Optional.of(result);
        } catch (XPathExpressionException e) {
            System.err.println(e.getMessage());
            return Optional.empty();
        }
    }
}

record ArtifactDependency(Gav cord, String scope, boolean optional) {}

record PomInfo(String packaging, List<ArtifactDependency> dependencies) {}
