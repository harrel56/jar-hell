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

    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
    private final ConcurrentHashMap<Gav, CompletableFuture<ArtifactTree>> processingMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<Gav, ConcurrentLinkedQueue<Runnable>> analysisEndCallbacks = new ConcurrentHashMap<>();

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

        return analyzeInternal(gav).whenComplete((value, ex) -> {
            if (ex != null) {
                logger.error("Error occurred during analysis of [{}]", gav, ex);
            }
        });
    }

    private CompletableFuture<ArtifactTree> analyzeInternal(Gav gav) {
        CompletableFuture<ArtifactTree> future = new CompletableFuture<>();
        CompletableFuture<ArtifactTree> currentFuture = processingMap.putIfAbsent(gav, future);
        if (currentFuture != null) {
            return currentFuture;
        }
        CompletableFuture.supplyAsync(() -> getArtifactTree(gav), executorService)
                .thenCompose(Function.identity()) // flatMap
                .whenComplete((value, ex) -> {
                    try {
                        if (ex == null) {
                            future.complete(value);
                            ConcurrentLinkedQueue<Runnable> callbacks = analysisEndCallbacks.computeIfAbsent(gav, k -> new ConcurrentLinkedQueue<>());
                            if (!callbacks.isEmpty()) {
                                logger.warn("Analysis of [{}] is completed, but there are still {} callbacks to be called", gav, callbacks.size());
                            }
                            callbacks.forEach(Runnable::run);
                            analysisEndCallbacks.remove(gav);
                        } else {
                            future.completeExceptionally(ex);
                        }
                    } catch (Exception e) {
                        logger.error("Error occurred when calling callbacks of [{}]", gav, e);
                        future.completeExceptionally(ex);
                    } finally {
                        processingMap.remove(gav, future);
                    }
                });
        return future;
    }

    private CompletableFuture<ArtifactTree> getArtifactTree(Gav gav) {
        return artifactRepository.find(gav)
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> computeArtifactTree(gav));
    }

    private CompletableFuture<ArtifactTree> computeArtifactTree(Gav gav) {
        Instant start = Instant.now();
        logger.info("START analysis of [{}]", gav);
        AnalysisOutput analysisOutput = analyzer.analyze(gav);
        Set<Gav> depsToBreak = analysisOutput.dependencies().cyclesToBreak().getOrDefault(gav.stripClassifier(), Set.of());
        logger.trace("Direct dependencies of {}: {}", gav, analysisOutput.dependencies().directDependencies());

        Map<Gav, CompletableFuture<DependencyInfo>> directDepFutures = HashMap.newHashMap(analysisOutput.dependencies().directDependencies().size());
        for (FlatDependency dep : analysisOutput.dependencies().directDependencies()) {
            if (depsToBreak.contains(dep.gav())) {
                logger.warn("CYCLE found, breaking dependency: {}", dep.gav());
                CompletableFuture<DependencyInfo> cyclicDep = CompletableFuture.supplyAsync(() -> {
                    ArtifactInfo depInfo = analyzer.analyzeWithoutDeps(dep.gav());
                    return new DependencyInfo(new ArtifactTree(depInfo, List.of()), dep.optional(), dep.scope());
                }, executorService);
                directDepFutures.put(dep.gav(), cyclicDep);
                // trigger analysis of the dependency as it might get never triggered
                analysisEndCallbacks.computeIfAbsent(gav, k -> new ConcurrentLinkedQueue<>())
                        .add(() -> analyzeInternal(dep.gav()).join());
                // restore the relation when dependency is analyzed
                analysisEndCallbacks.computeIfAbsent(dep.gav(), k -> new ConcurrentLinkedQueue<>())
                        .add(() -> artifactRepository.saveDependency(gav, dep));
            } else {
                var depFuture = analyzeInternal(dep.gav()).thenApply(at -> new DependencyInfo(at, dep.optional(), dep.scope()));
                directDepFutures.put(dep.gav(), depFuture);
            }
        }

        return CompletableFuture.allOf(directDepFutures.values().toArray(CompletableFuture[]::new))
                .thenApply(ignored ->
                        analysisOutput.dependencies().allDependencies().stream()
                                .map(dep ->
                                        Optional.ofNullable(directDepFutures.get(dep.gav()))
                                                .orElseGet(() -> analyzeInternal(dep.gav()).thenApply(at -> new DependencyInfo(at, dep.optional(), dep.scope()))))
                                .map(CompletableFuture::join)
                                .toList()
                )
                .thenApply(allDeps -> {
                    List<DependencyInfo> directDeps = directDepFutures.values().stream().map(CompletableFuture::join).toList();
                    ArtifactTree artifactTree = new ArtifactTree(analysisOutput.artifactInfo(), directDeps);

                    Long totalSize = allDeps.stream().reduce(0L, (acc, dep) -> acc + dep.artifact().artifactInfo().getPackageSize(), Long::sum);
                    artifactRepository.save(artifactTree.withTotalSize(totalSize + artifactTree.artifactInfo().getPackageSize()));

                    long timeElapsed = Duration.between(start, Instant.now()).toMillis();
                    logger.info("END analysis of [{}] - completed in {}ms", gav, timeElapsed);
                    return artifactTree;
                });
    }
}
