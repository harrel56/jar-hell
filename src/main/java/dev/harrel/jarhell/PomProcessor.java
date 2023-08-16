package dev.harrel.jarhell;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.model.PomInfo;
import dev.harrel.jarhell.model.pom.ProjectModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class PomProcessor {
    private static final Logger logger = LoggerFactory.getLogger(PomProcessor.class);

    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public PomProcessor(HttpClient httpClient) {
        this.httpClient = httpClient;
        this.mapper = new XmlMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    PomInfo processPom(Gav gav) throws IOException, InterruptedException {
        String groupPath = gav.groupId().replace('.', '/');
        String fileName = "%s-%s.pom".formatted(gav.artifactId(), gav.version());
        String query = "?filepath=%s/%s/%s/%s".formatted(groupPath, gav.artifactId(), gav.version(), fileName);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://search.maven.org/remotecontent" + query))
                .GET()
                .build();
        HttpResponse<InputStream> inputResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        ProjectModel projectModel = mapper.readValue(inputResponse.body(), ProjectModel.class);
        return new PomInfo(projectModel.packaging(), projectModel.dependencies());
    }
}

