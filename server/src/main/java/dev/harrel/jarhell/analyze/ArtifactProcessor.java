package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.repo.ArtifactRepository;
import dev.harrel.jarhell.util.ConcurrentUtil;
import io.avaje.config.Config;
import io.avaje.inject.PostConstruct;
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

public class ArtifactProcessor implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(ArtifactProcessor.class);

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

    @PostConstruct
    public void postConstruct() {
        if (!Config.enabled("jar-hell.dev-mode", false)) {
            start(1);
        }
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
        List<Gav> unresolvedGavs = repo.findAllUnresolved(concurrency.get(), 3);
        if (!unresolvedGavs.isEmpty()) {
            logger.info("Fetched {} gavs for reanalysis [unresolved]", unresolvedGavs.size());
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                unresolvedGavs.forEach(gav -> scope.fork(() -> analyzeEngine.doFullAnalysis(gav)));
                ConcurrentUtil.joinScope(scope);
            }
            counter.addAndGet(unresolvedGavs.size());
            return unresolvedGavs.size();
        }
        return 0;
    }
}
