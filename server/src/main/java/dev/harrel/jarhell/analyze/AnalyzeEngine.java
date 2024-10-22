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
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        private final ConcurrentMap<Gav, ConcurrentLinkedQueue<Runnable>> analysisEndCallbacks = new ConcurrentHashMap<>();

        private CompletableFuture<ArtifactTree> analyzeInternal(Gav gav, AnalysisChain chain) {
            CompletableFuture<ArtifactTree> future = new CompletableFuture<>();
            CompletableFuture<ArtifactTree> currentFuture = processingMap.putIfAbsent(gav, future);
            if (currentFuture != null) {
                return currentFuture;
            }
            CompletableFuture.supplyAsync(() -> getArtifactTree(gav, chain), executorService)
                    .thenCompose(Function.identity()) // flatMap
                    .whenComplete((value, ex) -> {
                        try {
                            if (ex == null) {
                                future.complete(value);
                                // todo: arraylist is not thread-safe, use locks?
                                ConcurrentLinkedQueue<Runnable> callbacks = analysisEndCallbacks.computeIfAbsent(gav, k -> new ConcurrentLinkedQueue<>());
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

        private CompletableFuture<ArtifactTree> getArtifactTree(Gav gav, AnalysisChain chain) {
            return artifactRepository.find(gav)
                    .map(CompletableFuture::completedFuture)
                    .orElseGet(() -> computeArtifactTree(gav, chain));
        }

        private CompletableFuture<ArtifactTree> computeArtifactTree(Gav gav, AnalysisChain chain) {
            Instant start = Instant.now();
            logger.info("START analysis of [{}]", gav);
            AnalysisOutput analysisOutput = analyzer.analyze(gav);
            Set<Gav> depsToBreak = analysisOutput.dependencies().cyclesToBreak().getOrDefault(gav.stripClassifier(), Set.of());
            logger.info("Direct dependencies of {}: {}", gav, analysisOutput.dependencies().directDependencies());

            List<CompletableFuture<DependencyInfo>> directDepFutures = new ArrayList<>(analysisOutput.dependencies().directDependencies().size());
            for (FlatDependency dep : analysisOutput.dependencies().directDependencies()) {
                if (depsToBreak.contains(dep.gav())) {
                    logger.warn("CYCLE found, breaking dependency: {}", dep.gav());
                    CompletableFuture<DependencyInfo> cyclicDep = CompletableFuture.supplyAsync(() -> {
                        ArtifactInfo depInfo = analyzer.analyzeWithoutDeps(dep.gav());
                        return new DependencyInfo(new ArtifactTree(depInfo, List.of()), dep.optional(), dep.scope());
                    }, executorService);
                    directDepFutures.add(cyclicDep);
                    // trigger analysis of the dependency as it might get never triggered
                    analysisEndCallbacks.computeIfAbsent(gav, k -> new ConcurrentLinkedQueue<>())
                            .add(() -> analyzeInternal(dep.gav(), AnalysisChain.start(dep.gav())).join());
                    // restore the relation when dependency is analyzed
                    analysisEndCallbacks.computeIfAbsent(dep.gav(), k -> new ConcurrentLinkedQueue<>())
                            .add(() -> artifactRepository.saveDependency(gav, dep));
                } else {
                    var depFuture = analyzeInternal(dep.gav(), chain.nextNode(dep)).thenApply(at -> new DependencyInfo(at, dep.optional(), dep.scope()));
                    directDepFutures.add(depFuture);
                }
//                AnalysisChain newChain = chain.nextNode(dep);
//                AnalysisChain.CycleData cycleData = newChain.checkCycle();
//                if (cycleData == null) {
//                    var depFuture = analyzeInternal(dep.gav(), newChain).thenApply(at -> new DependencyInfo(at, dep.optional(), dep.scope()));
//                    directDepFutures.add(depFuture);
//                } else {
//                    logger.warn("{} CYCLE found, preceding: {}, cycle: {}", cycleData.hard() ? "HARD" : "SOFT", cycleData.preceding(), cycleData.cycle());
//                    analysisEndCallbacks.computeIfAbsent(dep.gav(), k -> new ArrayList<>())
//                            .add(() -> artifactRepository.saveDependency(gav, dep));
//                    ArtifactInfo info = new ArtifactInfo(dep.gav().groupId(), dep.gav().artifactId(), dep.gav().version(), dep.gav().classifier());
//                    DependencyInfo di = new DependencyInfo(new ArtifactTree(info, List.of()), dep.optional(), dep.scope());
//                    directDepFutures.add(CompletableFuture.completedFuture(di));
//                }
            }

            return CompletableFuture.allOf(directDepFutures.toArray(CompletableFuture[]::new))
                    .thenApply(ignored -> {
                        Map<Gav, ArtifactTree> directDeps = directDepFutures.stream().map(CompletableFuture::join)
                                        .collect(Collectors.toMap(di -> new Gav(di.artifact().artifactInfo().groupId(),
                                                di.artifact().artifactInfo().artifactId(), di.artifact().artifactInfo().version(),
                                                di.artifact().artifactInfo().classifier()), DependencyInfo::artifact));
//                        return List.of();
                        return analysisOutput.dependencies().allDependencies().stream()
                                .map(dep -> Optional.ofNullable(directDeps.get(dep.gav()))
                                        .map(CompletableFuture::completedFuture)
                                        .orElseGet(() -> analyzeInternal(dep.gav(), AnalysisChain.start(dep.gav()))))
                                .map(CompletableFuture::join)
                                .toList();

//                        return analysisOutput.dependencies().allDependencies().stream()
//                                .map(dep -> analyzeInternal(dep.gav(), AnalysisChain.start(dep.gav())))
//                                .map(CompletableFuture::join)
//                                .toList();
                    })
                    .thenApply(allDeps -> {
                        List<DependencyInfo> directDeps = directDepFutures.stream().map(CompletableFuture::join).collect(Collectors.toCollection(ArrayList::new));

                        ArtifactTree artifactTree = new ArtifactTree(analysisOutput.artifactInfo(), directDeps);
//                        Analyzer.TraversalOutput to = analyzer.adjustArtifactTree(artifactTree, allDeps);
//                        logger.warn("Traversal output: {}", to);

                        Long totalSize = allDeps.stream().reduce(0L, (acc, dep) -> acc + (dep.artifactInfo().packageSize() == null ? 0 : dep.artifactInfo().packageSize()), Long::sum);
//                        Long totalSize = analyzer.calculateTotalSize(artifactTree);
                        artifactRepository.save(artifactTree.withTotalSize(totalSize + artifactTree.artifactInfo().packageSize()));

                        long timeElapsed = Duration.between(start, Instant.now()).toMillis();
                        logger.info("END analysis of [{}] - completed in {}ms", gav, timeElapsed);
                        return artifactTree;
                    });
        }
    }
}
