package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.analyze.Analyzer.AnalysisOutput;
import dev.harrel.jarhell.error.ResourceNotFoundException;
import dev.harrel.jarhell.model.*;
import dev.harrel.jarhell.repo.ArtifactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

@Singleton
public class AnalyzeEngine {
    private static final Logger logger = LoggerFactory.getLogger(AnalyzeEngine.class);

    private final ExecutorService executorService = Executors.newFixedThreadPool(32);
    private final ConcurrentHashMap<Gav, CompletableFuture<ArtifactTree>> processingMap = new ConcurrentHashMap<>();

    private final ArtifactRepository artifactRepository;
    private final Analyzer analyzer;

    AnalyzeEngine(ArtifactRepository artifactRepository, Analyzer analyzer) {
        this.artifactRepository = artifactRepository;
        this.analyzer = analyzer;
    }

    public CompletableFuture<ArtifactTree> analyze(Gav gav) {
        Optional<ArtifactTree> artifactTree = artifactRepository.find(gav);
        if (artifactTree.isPresent()) {
            logger.info("Analysis of [{}] is not necessary", gav);
            return CompletableFuture.completedFuture(artifactTree.get());
        }
        if (!analyzer.checkIfArtifactExists(gav)) {
            throw new ResourceNotFoundException(gav);
        }

        return new RequestScope().analyzeInternal(gav, AnalysisChain.start(gav)).whenComplete((value, ex) -> {
            if (ex != null) {
                logger.error("Error occurred during analysis of [{}]", gav, ex);
            }
        });
    }

    private class RequestScope {
        private final ConcurrentMap<Gav, List<Runnable>> analysisEndCallbacks = new ConcurrentHashMap<>();

        private CompletableFuture<ArtifactTree> analyzeInternal(Gav gav, AnalysisChain chain) {
            CompletableFuture<ArtifactTree> future = new CompletableFuture<>();
            CompletableFuture<ArtifactTree> currentFuture = processingMap.putIfAbsent(gav, future);
            if (currentFuture != null) {
                return currentFuture;
            }
            CompletableFuture.supplyAsync(() -> getArtifactTree(gav, chain), executorService)
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
        }

        private CompletableFuture<ArtifactTree> getArtifactTree(Gav gav, AnalysisChain chain) {
            return artifactRepository.find(gav)
                    .map(CompletableFuture::completedFuture)
                    .orElseGet(() -> computeArtifactTree(gav, chain));
        }

        private CompletableFuture<ArtifactTree> computeArtifactTree(Gav gav, AnalysisChain chain) {
            Instant start = Instant.now();
            logger.info("START analysis of [{}]", gav);
            AnalysisOutput analysisOutput = analyzer.analyze(gav);
            logger.info("Direct dependencies of {}: {}", gav, analysisOutput.dependencies());

            List<CompletableFuture<DependencyInfo>> depFutures = new ArrayList<>(analysisOutput.dependencies().size());
            for (FlatDependency dep : analysisOutput.dependencies()) {
                AnalysisChain newChain = chain.nextNode(dep);
                AnalysisChain.CycleData cycleData = newChain.checkCycle();
                if (cycleData == null) {
                    var depFuture = analyzeInternal(dep.gav(), newChain).thenApply(at -> new DependencyInfo(at, dep.optional(), dep.scope()));
                    depFutures.add(depFuture);
                } else {
                    if (cycleData.hard()) {
                        String msg = "HARD CYCLE found, preceding: %s, cycle: %s".formatted(cycleData.preceding(), cycleData.cycle());
                        logger.error(msg);
                        throw new IllegalArgumentException(msg);
                    }
                    logger.warn("SOFT CYCLE found, preceding: {}, cycle: {}", cycleData.preceding(), cycleData.cycle());
                    analysisEndCallbacks.computeIfAbsent(dep.gav(), k -> new ArrayList<>())
                            .add(() -> artifactRepository.saveDependency(gav, dep));
                }
            }

            return CompletableFuture.allOf(depFutures.toArray(CompletableFuture[]::new))
                    .thenApply(nothing ->
                            depFutures.stream()
                                    .map(CompletableFuture::join)
                                    .toList()
                    )
                    .thenApply(deps -> {
                        ArtifactTree artifactTree = new ArtifactTree(analysisOutput.artifactInfo(), deps);
                        Long totalSize = analyzer.calculateTotalSize(artifactTree);
                        artifactRepository.save(artifactTree.withTotalSize(totalSize));
                        List<Runnable> callbacks = analysisEndCallbacks.computeIfAbsent(gav, k -> new ArrayList<>());
                        callbacks.forEach(Runnable::run);
                        analysisEndCallbacks.remove(gav);

                        long timeElapsed = Duration.between(start, Instant.now()).toMillis();
                        logger.info("END analysis of [{}] - completed in {}ms", gav, timeElapsed);
                        return artifactTree;
                    });
        }
    }
}
