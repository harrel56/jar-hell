package dev.harrel.jarhell;

import dev.harrel.jarhell.model.ArtifactInfo;
import dev.harrel.jarhell.model.ArtifactTree;
import dev.harrel.jarhell.model.DependencyInfo;
import dev.harrel.jarhell.model.Gav;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
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
import java.util.concurrent.*;
import java.util.function.Function;

public class AnalyzeHandler implements Handler {
    private static final Logger logger = LoggerFactory.getLogger(AnalyzeHandler.class);

    private final ArtifactRepository artifactRepository;
    private final Analyzer analyzer;
    private final DependencyResolver dependencyResolver;

    private final ExecutorService executorService = Executors.newFixedThreadPool(32);
    private final ConcurrentHashMap<Gav, CompletableFuture<ArtifactTree>> processingMap = new ConcurrentHashMap<>();

    public AnalyzeHandler(ArtifactRepository artifactRepository, Analyzer analyzer, DependencyResolver dependencyResolver) {
        this.artifactRepository = artifactRepository;
        this.analyzer = analyzer;
        this.dependencyResolver = dependencyResolver;
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

        CompletableFuture<ArtifactTree> artifactTree = getArtifactTree(gav);
        ctx.json(artifactTree.join());
    }

    private CompletableFuture<ArtifactTree> getArtifactTree(Gav gav) {
        return artifactRepository.find(gav)
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> guardedComputeArtifactTree(gav));
    }

    private CompletableFuture<ArtifactTree> guardedComputeArtifactTree(Gav gav) {
        CompletableFuture<ArtifactTree> future = new CompletableFuture<>();
        CompletableFuture<ArtifactTree> currentFuture = processingMap.putIfAbsent(gav, future);
        if (currentFuture == null) {
            CompletableFuture.supplyAsync(() -> computeArtifactTree(gav), executorService)
                    .thenCompose(Function.identity()) // flatMap
                    .thenAccept(future::complete);
            return future;
        } else {
            return currentFuture;
        }
    }

    private CompletableFuture<ArtifactTree> computeArtifactTree(Gav gav) {
        try {
            Instant start = Instant.now();
            logger.info("Starting analysis of [{}]", gav);
            ArtifactInfo artifactInfo = analyzer.analyze(gav);
            DependencyNode dependencyNode = dependencyResolver.resolveDependencies(gav);

            List<CompletableFuture<DependencyInfo>> futures = dependencyNode.getChildren().stream()
                    .map(DependencyNode::getDependency)
                    .map(this::computeDependency)
                    .toList();

            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                    .thenApply(nothing ->
                                    futures.stream()
                                            .map(CompletableFuture::join)
                                            .toList()
                            )
                    .thenApply(deps -> {
                        ArtifactTree artifactTree = new ArtifactTree(artifactInfo, deps);
                        artifactRepository.save(artifactTree);
                        long timeElapsed = Duration.between(start, Instant.now()).toMillis();
                        logger.info("Analysis of [{}] completed in {}ms", gav, timeElapsed);
                        return artifactTree;
                    });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException(e);
        }
    }

    private CompletableFuture<DependencyInfo> computeDependency(Dependency dep) {
        Artifact artifact = dep.getArtifact();
        Gav depGav = new Gav(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
        String scope = dep.getScope().isBlank() ? "compile" : dep.getScope();
        // scope "system" is broken and this dep might not exist at all
        if (scope.equals("system")) {
            ArtifactInfo stubInfo = new ArtifactInfo(depGav.groupId(), depGav.artifactId(), depGav.version(),
                    null, null, null, null, null, null, null);
            DependencyInfo dependencyInfo = new DependencyInfo(new ArtifactTree(stubInfo, List.of()), dep.getOptional(), scope);
            return CompletableFuture.completedFuture(dependencyInfo);
        }
        return getArtifactTree(depGav).thenApply(at -> new DependencyInfo(at, dep.getOptional(), scope));
    }
}
