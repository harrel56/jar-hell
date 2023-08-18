package dev.harrel.jarhell;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.harrel.jarhell.model.ArtifactInfo;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.neo4j.driver.Values.parameters;

public class AnalyzeHandler implements Handler {
    private static final Logger logger = LoggerFactory.getLogger(AnalyzeHandler.class);

    private final Processor processor;

    public AnalyzeHandler(Processor processor) {
        this.processor = processor;
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        Instant start = Instant.now();
        String groupId = Optional.ofNullable(ctx.queryParam("groupId"))
                .orElseThrow(() -> new IllegalArgumentException("Argument 'groupId' is required"));
        String artifactId = Optional.ofNullable(ctx.queryParam("artifactId"))
                .orElseThrow(() -> new IllegalArgumentException("Argument 'artifactId' is required"));
        ArtifactInfo artifactInfo = processor.process(groupId, artifactId);

        ObjectMapper mapper = ctx.appAttribute(Attributes.OBJECT_MAPPER);
        Map<String, Object> dataMap = mapper.convertValue(artifactInfo, new TypeReference<>() {});
        Driver driver = ctx.appAttribute(Attributes.NEO4J_DRIVER);
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(tx -> tx.run(new Query("CREATE (a:Artifact $map) RETURN id(a)",
                    parameters("map", dataMap))
                )
            );
        }

        ctx.json(artifactInfo);

        long timeElapsed = Duration.between(start, Instant.now()).toMillis();
        logger.info("Analysis of [{}] completed in {}ms", artifactInfo.toGav(), timeElapsed);
    }
}
