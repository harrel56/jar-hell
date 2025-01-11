package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.repo.ArtifactRepository;
import dev.harrel.jarhell.util.ConcurrentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.StructuredTaskScope;

public class UnresolvedAnalyzerTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(UnresolvedAnalyzerTask.class);

    private final ArtifactRepository repo;
    private final AnalyzeEngine analyzeEngine;

    public UnresolvedAnalyzerTask(ArtifactRepository repo, AnalyzeEngine analyzeEngine) {
        this.repo = repo;
        this.analyzeEngine = analyzeEngine;
    }

    @Override
    public void run() {
        try {
            doRun();
        } catch (Exception e) {
            logger.warn("Task failed", e);
        }
    }

    private void doRun() {
        Instant startTime = Instant.now();
        List<Gav> unresolvedGavs = repo.findAllUnresolved(64, 3);
        logger.info("Fetched {} gavs for reanalysis", unresolvedGavs.size());
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            unresolvedGavs.forEach(analyzeEngine::doFullAnalysis);
            ConcurrentUtil.joinScope(scope);
        }
        logger.info("Task finished in {}s", Duration.between(startTime, Instant.now()).toSeconds());
    }
}
