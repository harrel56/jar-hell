package dev.harrel.jarhell.analyze;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.harrel.jarhell.ArtifactRepository;
import dev.harrel.jarhell.model.ArtifactInfo;
import dev.harrel.jarhell.model.ArtifactTree;
import dev.harrel.jarhell.model.DependencyInfo;
import dev.harrel.jarhell.model.Gav;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class AnalyzeEngine {
    private static final Logger logger = LoggerFactory.getLogger(AnalyzeEngine.class);

    private final ExecutorService executorService = Executors.newFixedThreadPool(32);
    private final ConcurrentHashMap<Gav, CompletableFuture<ArtifactTree>> processingMap = new ConcurrentHashMap<>();

    private final ArtifactRepository artifactRepository;
    private final MavenRunner mavenRunner;
    private final Analyzer analyzer;

    public AnalyzeEngine(ObjectMapper objectMapper, ArtifactRepository artifactRepository) {
        this.artifactRepository = artifactRepository;
        this.mavenRunner = new MavenRunner();
        this.analyzer = new Analyzer(objectMapper, mavenRunner);
    }

    public CompletableFuture<ArtifactTree> analyze(Gav gav) {
        CompletableFuture<ArtifactTree> future = new CompletableFuture<>();
        CompletableFuture<ArtifactTree> currentFuture = processingMap.putIfAbsent(gav, future);
        if (currentFuture == null) {
            CompletableFuture.supplyAsync(() -> getArtifactTree(gav), executorService)
                    .thenCompose(Function.identity()) // flatMap
                    .whenComplete((value, ex) -> {
                        if (ex == null) {
                            future.complete(value);
                        } else {
                            future.completeExceptionally(ex);
                        }
                        processingMap.remove(gav, future);
                    });
            return future;
        } else {
            return currentFuture;
        }
    }

    private CompletableFuture<ArtifactTree> getArtifactTree(Gav gav) {
        return artifactRepository.find(gav)
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> computeArtifactTree(gav));
    }

    private CompletableFuture<ArtifactTree> computeArtifactTree(Gav gav) {
        Instant start = Instant.now();
        logger.info("Starting analysis of [{}]", gav);
        ArtifactInfo artifactInfo = analyzer.analyze(gav);
        DependencyNode dependencyNode = mavenRunner.resolveDependencies(gav);

        List<CompletableFuture<DependencyInfo>> futures = dependencyNode.getChildren().stream()
                .map(DependencyNode::getDependency)
                .map(this::computeDependencyInfo)
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
    }

    private CompletableFuture<DependencyInfo> computeDependencyInfo(Dependency dep) {
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
        return analyze(depGav).thenApply(at -> new DependencyInfo(at, dep.getOptional(), scope));
    }
}
