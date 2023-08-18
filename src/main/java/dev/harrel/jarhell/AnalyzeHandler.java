package dev.harrel.jarhell;

import dev.harrel.jarhell.model.ArtifactInfo;
import dev.harrel.jarhell.model.Gav;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class AnalyzeHandler implements Handler {
    private static final Logger logger = LoggerFactory.getLogger(AnalyzeHandler.class);

    private final ArtifactRepository artifactRepository;
    private final Processor processor;

    public AnalyzeHandler(ArtifactRepository artifactRepository, Processor processor) {
        this.artifactRepository = artifactRepository;
        this.processor = processor;
    }

    @Override
    public void handle(@NotNull Context ctx) {
        String groupId = Optional.ofNullable(ctx.queryParam("groupId"))
                .orElseThrow(() -> new IllegalArgumentException("Argument 'groupId' is required"));
        String artifactId = Optional.ofNullable(ctx.queryParam("artifactId"))
                .orElseThrow(() -> new IllegalArgumentException("Argument 'artifactId' is required"));
        String version = Optional.ofNullable(ctx.queryParam("version"))
                .orElseThrow(() -> new IllegalArgumentException("Argument 'version' is required"));
        Gav gav = new Gav(groupId, artifactId, version);
        Optional<ArtifactInfo> storedInfo = artifactRepository.find(gav);
        ArtifactInfo artifactInfo = storedInfo.orElseGet(() -> computeArtifactInfo(gav));
        ctx.json(artifactInfo);
    }

    private ArtifactInfo computeArtifactInfo(Gav gav) {
        try {
            Instant start = Instant.now();
            logger.info("Starting analysis of [{}]", gav);
            ArtifactInfo artifactInfo = processor.process(gav);
            artifactRepository.save(artifactInfo);
            long timeElapsed = Duration.between(start, Instant.now()).toMillis();
            logger.info("Analysis of [{}] completed in {}ms", gav, timeElapsed);
            return artifactInfo;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException(e);
        }
    }
}
