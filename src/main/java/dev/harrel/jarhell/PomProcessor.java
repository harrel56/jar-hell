package dev.harrel.jarhell;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.model.PomInfo;
import dev.harrel.jarhell.model.pom.ProjectModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class PomProcessor {
    private static final Logger logger = LoggerFactory.getLogger(PomProcessor.class);

    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public PomProcessor(HttpClient httpClient) {
        this.httpClient = httpClient;
        this.mapper = new XmlMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    PomInfo computePomInfo(Gav gav) throws IOException, InterruptedException {
        ProjectModel projectModel = fetchProjectModel(gav);

        if (projectModel.parent() != null) {

        }

        return new PomInfo(projectModel.packaging(), projectModel.dependencies());
    }

    private ProjectModel fetchProjectModel(Gav gav) throws IOException, InterruptedException {
        String groupPath = gav.groupId().replace('.', '/');
        String fileName = "%s-%s.pom".formatted(gav.artifactId(), gav.version());
        String query = "?filepath=%s/%s/%s/%s".formatted(groupPath, gav.artifactId(), gav.version(), fileName);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://search.maven.org/remotecontent" + query))
                .GET()
                .build();
        String response = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();

//        String response = """
//                <project>
//                    <properties>
//                        <what.elo>1.1.1</what.elo>
//                    </properties>
//                    <dependencies>
//                        <dependency>
//                            <groupId>org.jetbrains</groupId>
//                            <artifactId>annotations</artifactId>
//                            <version>${what.elo}</version>
//                        </dependency>
//                    </dependencies>
//                </project>
//                """;
        ProjectModel rawModel = mapper.readValue(response, ProjectModel.class);
        if (rawModel.properties() == null) {
            return rawModel;
        } else {
            String pomWithProps = injectProperties(response, rawModel.properties());
            return mapper.readValue(pomWithProps, ProjectModel.class);
        }
    }

    // todo: this is very naive
    private String injectProperties(String rawPom, Map<String, String> props) {
        for (Map.Entry<String, String> prop : props.entrySet()) {
            rawPom = rawPom.replace("${%s}".formatted(prop.getKey()), prop.getValue());
        }
        return rawPom;
    }
}

