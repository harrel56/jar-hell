package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.error.ResourceNotFoundException;
import dev.harrel.jarhell.model.*;
import dev.harrel.jarhell.repo.ArtifactRepository;
import dev.harrel.jarhell.util.ConcurrentUtil;
import dev.harrel.jarhell.util.ParametrizedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Subtask;

@Singleton
public class AnalyzeEngine {
    private static final Logger logger = LoggerFactory.getLogger(AnalyzeEngine.class);

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

        return CompletableFuture.supplyAsync(() -> doFullAnalysis(gav), Executors.newVirtualThreadPerTaskExecutor());
    }

    public void saveUnresolved(Gav gav) {
        if (!artifactRepository.exists(gav)) {
            artifactRepository.saveArtifact(ArtifactInfo.unresolved(gav));
        }
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

        List<DependencyInfo> directDeps;
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            List<Subtask<DependencyInfo>> partialDepTasks = output.dependencies().directDependencies().stream()
                    .map(dep -> scope.fork(() -> {
                        ArtifactTree depTree = doFullAnalysis(dep.gav());
                        return new DependencyInfo(depTree, dep.optional(), dep.scope());
                    }))
                    .toList();
            ConcurrentUtil.joinScope(scope);
            directDeps = partialDepTasks.stream().map(Subtask::get).toList();
        }

        artifactRepository.saveDependencies(gav, output.dependencies().directDependencies());
        // todo: possibly not removed, maybe remove in finally
        partialAnalysis.remove(gav);
        logger.info("END FULL analysis of [{}]", gav);
        return new ArtifactTree(output.artifactInfo(), directDeps);
    }

    private AnalysisOutput doBaseAnalysis(Gav gav) {
        logger.info("START BASE analysis of [{}]", gav);
        ArtifactInfo info = analyzePartially(gav);

        List<DependencyInfo> partialDeps;
        CollectedDependencies deps = Boolean.TRUE.equals(info.unresolved()) ? CollectedDependencies.empty() : analyzer.analyzeDeps(gav);
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            List<Subtask<DependencyInfo>> partialDepTasks = deps.allDependencies().stream()
                    .map(dep -> scope.fork(() -> {
                        var artifactInfo = analyzePartially(dep.gav());
                        return new DependencyInfo(new ArtifactTree(artifactInfo, List.of()), dep.optional(), dep.scope());
                    }))
                    .toList();
            ConcurrentUtil.joinScope(scope);
            partialDeps = partialDepTasks.stream().map(Subtask::get).toList();
        }

        ArtifactInfo.EffectiveValues effectiveValues = analyzer.computeEffectiveValues(info, partialDeps);
        info = info.withEffectiveValues(effectiveValues);

        artifactRepository.saveArtifact(info);

        logger.info("END BASE analysis of [{}]", gav);
        return new AnalysisOutput(info, deps, effectiveValues);
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
