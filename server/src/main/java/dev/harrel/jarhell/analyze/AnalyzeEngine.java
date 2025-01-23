package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.model.*;
import dev.harrel.jarhell.repo.ArtifactRepository;
import dev.harrel.jarhell.util.ParametrizedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

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
        Optional<ArtifactTree> artifactTree = artifactRepository.find(gav)
                .filter(at -> !Boolean.TRUE.equals(at.artifactInfo().unresolved()));
        if (artifactTree.isPresent()) {
            logger.info("Analysis of [{}] is not necessary", gav);
            return CompletableFuture.completedFuture(artifactTree.get());
        }

        return CompletableFuture.supplyAsync(() -> doFullAnalysis(gav), Executors.newVirtualThreadPerTaskExecutor());
    }

    public void saveUnresolved(Gav gav) {
        if (!artifactRepository.exists(gav)) {
            artifactRepository.saveArtifact(ArtifactInfo.unresolved(gav, "crawl"));
        }
    }

    ArtifactTree doFullAnalysis(Gav gav) {
        try {
            logger.info("START FULL analysis of [{}]", gav);
            AnalysisOutput output;
            lock.lock(gav);
            try {
                Optional<ArtifactTree> artifactTree = artifactRepository.find(gav)
                        .filter(at -> !Boolean.TRUE.equals(at.artifactInfo().unresolved()));
                if (artifactTree.isPresent()) {
                    return artifactTree.get();
                }
                output = doBaseAnalysis(gav);
            } finally {
                lock.unlock(gav);
            }

            for (FlatDependency dep : output.dependencies().directDependencies()) {
                doFullAnalysis(dep.gav());
            }

            artifactRepository.saveDependencies(gav, output.dependencies().directDependencies());
            logger.info("END FULL analysis of [{}]", gav);
            return new ArtifactTree(output.artifactInfo(), List.of());
        } catch (Exception e) {
            logger.warn("Analysis of [{}] failed", gav, e);
            throw e;
        } finally {
            partialAnalysis.remove(gav);
        }
    }

    private AnalysisOutput doBaseAnalysis(Gav gav) {
        logger.info("START BASE analysis of [{}]", gav);
        ArtifactInfo info = analyzePartially(gav);

        CollectedDependencies deps = Boolean.TRUE.equals(info.unresolved()) ? CollectedDependencies.empty() : analyzer.analyzeDeps(gav);
        List<DependencyInfo> partialDeps = deps.allDependencies().stream()
                .map(dep -> {
                    var artifactInfo = analyzePartially(dep.gav());
                    return new DependencyInfo(new ArtifactTree(artifactInfo, List.of()), dep.optional(), dep.scope());
                })
                .toList();

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
