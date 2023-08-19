package dev.harrel.jarhell;

import dev.harrel.jarhell.model.ArtifactInfo;
import dev.harrel.jarhell.model.ArtifactTree;
import dev.harrel.jarhell.model.DependencyInfo;
import dev.harrel.jarhell.model.Gav;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class AnalyzeHandler implements Handler {
    private static final Logger logger = LoggerFactory.getLogger(AnalyzeHandler.class);

    private final ArtifactRepository artifactRepository;
    private final Analyzer analyzer;
    private final DependencyResolver dependencyResolver;

    public AnalyzeHandler(ArtifactRepository artifactRepository, Analyzer analyzer) {
        this.artifactRepository = artifactRepository;
        this.analyzer = analyzer;
        this.dependencyResolver = new DependencyResolver();
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

        ArtifactTree artifactTree = getArtifactTree(gav);
        ctx.json(artifactTree);
    }

    private ArtifactTree getArtifactTree(Gav gav) {
        return artifactRepository.find(gav).orElseGet(() -> computeArtifactTree(gav));
    }

    private ArtifactTree computeArtifactTree(Gav gav) {
        try {
            Instant start = Instant.now();
            logger.info("Starting analysis of [{}]", gav);
            // todo: process concurrently
            ArtifactInfo artifactInfo = analyzer.analyze(gav);
            DependencyNode dependencyNode = dependencyResolver.resolveDependencies(gav);
            List<DependencyInfo> deps = dependencyNode.getChildren().stream()
                    .map(DependencyNode::getDependency)
                    .map(dep -> {
                        Artifact artifact = dep.getArtifact();
                        Gav depGav = new Gav(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
                        String scope = dep.getScope().isBlank() ? "compile" : dep.getScope();
                        return new DependencyInfo(getArtifactTree(depGav), dep.getOptional(), scope);
                    })
                    .toList();

            ArtifactTree artifactTree = new ArtifactTree(artifactInfo, deps);
            artifactRepository.save(artifactTree);
            long timeElapsed = Duration.between(start, Instant.now()).toMillis();
            logger.info("Analysis of [{}] completed in {}ms", gav, timeElapsed);
            return artifactTree;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException(e);
        }
    }
}
