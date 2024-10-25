package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.error.ResourceNotFoundException;
import dev.harrel.jarhell.model.*;
import dev.harrel.jarhell.repo.ArtifactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.StructuredTaskScope.Subtask;

@Singleton
public class AnalyzeEngine {
    private static final Logger logger = LoggerFactory.getLogger(AnalyzeEngine.class);

    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
    private final ParametrizedLock<Gav> lock = new ParametrizedLock<>();
    private final ConcurrentHashMap<Gav, ArtifactInfo> partialAnalysis = new ConcurrentHashMap<>();

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

        return CompletableFuture.supplyAsync(() -> doFullAnalysis(gav), executorService);
    }

    private ArtifactTree doFullAnalysis(Gav gav) {
        logger.info("START FULL analysis of [{}]", gav);
        AnalysisOutput output;
        lock.lock(gav);
        try {
            Optional<ArtifactTree> artifactTree = artifactRepository.find(gav);
            if (artifactTree.isPresent()) {
                return artifactTree.get();
            }
            output = doBaseAnalysis(gav);
        } finally {
            lock.unlock(gav);
        }

        // 5. full analysis of deps
        List<DependencyInfo> directDeps;
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            List<Subtask<DependencyInfo>> partialDepTasks = output.dependencies().directDependencies().stream()
                    .map(dep -> scope.fork(() -> {
                        ArtifactTree depTree = doFullAnalysis(dep.gav());
                        return new DependencyInfo(depTree, dep.optional(), dep.scope());
                    }))
                    .toList();
            joinScope(scope);
            directDeps = partialDepTasks.stream().map(Subtask::get).toList();
        }

        // 6. save relations todo: save all at once
        for (FlatDependency directDep : output.dependencies().directDependencies()) {
            artifactRepository.saveDependency(gav, directDep);
        }

        // 7. clean partial analysis
        partialAnalysis.remove(gav);

        logger.info("END FULL analysis of [{}]", gav);
        return new ArtifactTree(output.artifactInfo(), directDeps);
    }

    private AnalysisOutput doBaseAnalysis(Gav gav) {
        logger.info("START BASE analysis of [{}]", gav);
        ArtifactInfo info = analyzePartially(gav);

        List<ArtifactInfo> partialDeps;
        CollectedDependencies deps = analyzer.analyzeDeps(gav);
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            List<Subtask<ArtifactInfo>> partialDepTasks = deps.allDependencies().stream()
                    .filter(dep -> !dep.optional())
                    .map(dep -> scope.fork(() -> analyzePartially(dep.gav())))
                    .toList();
            joinScope(scope);
            partialDeps = partialDepTasks.stream().map(Subtask::get).toList();
        }

        // 3. effective values computation
        Long totalSize = info.getPackageSize() + partialDeps.stream().reduce(0L, (acc, dep) -> acc + dep.getPackageSize(), Long::sum);
        ArtifactInfo.EffectiveValues effectiveValues = new ArtifactInfo.EffectiveValues(totalSize);
        info = info.withEffectiveValues(effectiveValues);

        // 4. save to repository (no relations)
        // todo specialised repo method to save without deps
        artifactRepository.save(new ArtifactTree(info, List.of()));

        logger.info("END BASE analysis of [{}]", gav);
        return new AnalysisOutput(info, deps, effectiveValues);
    }

    private void joinScope(StructuredTaskScope.ShutdownOnFailure scope) {
        try {
            scope.join().throwIfFailed(CompletionException::new);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompletionException(e);
        }
    }

    private ArtifactInfo analyzePartially(Gav gav) {
        // computeIfAbsent cannot be used as it is long, blocking operation (stated in the javadoc)
        ArtifactInfo info = partialAnalysis.get(gav);
        if (info == null) {
            info = analyzer.analyzePackage(gav);
            partialAnalysis.put(gav, info);
        }
        return info;
    }

    private record AnalysisOutput(ArtifactInfo artifactInfo,
                                  CollectedDependencies dependencies,
                                  ArtifactInfo.EffectiveValues effectiveValues) {}
}
