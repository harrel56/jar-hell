package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.repo.ArtifactRepository;
import dev.harrel.jarhell.util.ConcurrentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.StructuredTaskScope.open;

public class ArtifactProcessor implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(ArtifactProcessor.class);
    private static final int UNRESOLVED_LIMIT = 3;

    private final ExecutorService service = Executors.newSingleThreadExecutor();
    private final AtomicReference<Future<?>> runFuture = new AtomicReference<>(CompletableFuture.completedFuture(null));
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger concurrency = new AtomicInteger(1);
    private final AtomicInteger counter = new AtomicInteger(0);
    private final ArtifactRepository repo;
    private final AnalyzeEngine analyzeEngine;

    public ArtifactProcessor(ArtifactRepository repo, AnalyzeEngine analyzeEngine) {
        this.repo = repo;
        this.analyzeEngine = analyzeEngine;
    }

    public void start(int concurrency) {
        if (running.get()) {
            throw new IllegalStateException("Already running");
        }
        running.set(true);
        this.concurrency.set(concurrency);
        runFuture.set(service.submit(this::run));
    }

    public void stop() {
        running.set(false);
        try {
            runFuture.get().get();
            logger.info("Processor has stopped");
        } catch (ExecutionException e) {
            throw new CompletionException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompletionException(e);
        }
    }

    @Override
    public void close() {
        logger.info("Shutting down...");
        service.shutdownNow();
        stop();
    }


    private void run() {
        while (running.get()) {
            Instant startTime = Instant.now();
            try {
                logger.info("Waiting for cooldown... (2min)");
                Thread.sleep(Duration.ofMinutes(2));
                int processed = doRun();
                if (processed == 0) {
                    logger.info("No work to be done. Sleeping for 30 minutes...");
                    Thread.sleep(Duration.ofMinutes(30));
                    continue;
                }
            } catch (InterruptedException e) {
                logger.info("Interrupted. Stopping...");
                Thread.currentThread().interrupt();
                running.set(false);
            } catch (Exception e) {
                logger.warn("Batch failed", e);
            }
            logger.info("Batch finished in {}s, processed so far: {}", Duration.between(startTime, Instant.now()).toSeconds(), counter.get());
        }
    }

    private int doRun() {
        List<Gav> unresolvedGavs = repo.findUnanalyzedCandidates(concurrency.get());
        logger.info("Fetched {} gavs for reanalysis [unanalyzed]", unresolvedGavs.size());
        if (!unresolvedGavs.isEmpty()) {
            return doAnalyze(unresolvedGavs);
        }

        unresolvedGavs = repo.findAllUnresolved(concurrency.get(), UNRESOLVED_LIMIT);
        logger.info("Fetched {} gavs for reanalysis [unresolved]", unresolvedGavs.size());
        if (!unresolvedGavs.isEmpty()) {
            return doAnalyze(unresolvedGavs);
        }

        unresolvedGavs = repo.findAllEffectivelyUnresolved(concurrency.get(), UNRESOLVED_LIMIT);
        logger.info("Fetched {} gavs for reanalysis [effectively-unresolved]", unresolvedGavs.size());
        if (!unresolvedGavs.isEmpty()) {
            return doAnalyze(unresolvedGavs);
        }
        return 0;
    }

    private int doAnalyze(List<Gav> gavs) {
        try (var scope = open(StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow())) {
            gavs.forEach(gav -> scope.fork(() -> analyzeEngine.doFullAnalysis(gav)));
            ConcurrentUtil.joinScope(scope);
        }
        counter.addAndGet(gavs.size());
        return gavs.size();
    }
}
