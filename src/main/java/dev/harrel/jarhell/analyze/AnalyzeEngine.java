package dev.harrel.jarhell.analyze;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.harrel.jarhell.repo.ArtifactRepository;
import dev.harrel.jarhell.model.ArtifactInfo;
import dev.harrel.jarhell.model.ArtifactTree;
import dev.harrel.jarhell.model.DependencyInfo;
import dev.harrel.jarhell.model.Gav;
import org.apache.commons.lang3.StringUtils;
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
        return analyzeInternal(gav).whenComplete((value, ex) -> {
                    if (ex != null) {
                        logger.error("Error occurred during analysis of [{}]", gav, ex);
                    }
                });
    }

    private CompletableFuture<ArtifactTree> analyzeInternal(Gav gav) {
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
        logger.info("START analysis of [{}]", gav);
        AnalysisOutput analysisOutput = doAnalyze(gav);

        List<CompletableFuture<DependencyInfo>> futures = analysisOutput.dependencies().stream()
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
                    ArtifactTree artifactTree = new ArtifactTree(analysisOutput.artifactInfo(), deps);
                    artifactRepository.save(artifactTree);
                    long timeElapsed = Duration.between(start, Instant.now()).toMillis();
                    logger.info("END analysis of [{}] - completed in {}ms", gav, timeElapsed);
                    return artifactTree;
                });
    }

    private AnalysisOutput doAnalyze(Gav gav) {
        try {
            var artifactInfo = analyzer.analyze(gav);
            var dependencyNodes = mavenRunner.resolveDependencies(gav).getChildren();
            return new AnalysisOutput(artifactInfo, dependencyNodes);
        } catch (ArtifactNotFoundException e) {
            logger.warn("Artifact not found", e);
            var artifactInfo = new ArtifactInfo(gav.groupId(), gav.artifactId(), gav.version(), gav.classifier());
            return new AnalysisOutput(artifactInfo, List.of());
        }
    }

    private CompletableFuture<DependencyInfo> computeDependencyInfo(Dependency dep) {
        Artifact artifact = dep.getArtifact();
        Gav depGav = new Gav(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), StringUtils.stripToNull(artifact.getClassifier()));
        String scope = dep.getScope().isBlank() ? "compile" : dep.getScope();
        return analyzeInternal(depGav).thenApply(at -> new DependencyInfo(at, dep.getOptional(), scope));
    }

    private record AnalysisOutput(ArtifactInfo artifactInfo, List<DependencyNode> dependencies) {}
}
