package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.repo.ArtifactRepository;
import io.avaje.config.Config;
import org.apache.maven.index.reader.ChunkReader;
import org.apache.maven.index.reader.IndexReader;
import org.apache.maven.index.reader.ResourceHandler;
import org.apache.maven.index.reader.WritableResourceHandler;
import org.apache.maven.index.reader.resource.PathWritableResourceHandler;
import org.apache.maven.index.reader.resource.UriResourceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
public class MavenIndexService {
    private static final Logger logger = LoggerFactory.getLogger(MavenIndexService.class);
    private static final String UNRESOLVED_REASON = "initial-indexing";
    private static final String INDEX_PROPERTIES = "nexus-maven-repository-index.properties";
    private static final int BATCH_SIZE = 1_000;

    private final ArtifactRepository repo;
    private final AtomicBoolean running = new AtomicBoolean();

    MavenIndexService(ArtifactRepository repo) {
        this.repo = repo;
    }

    public void scanIndex() {
        if (!running.compareAndSet(false, true)) {
            logger.warn("Index scanning is already in progress, skipping");
            return;
        }
        try {
            doScanIndex();
        } finally {
            running.set(false);
        }
    }

    private void doScanIndex() {
        Instant start = Instant.now();
        Path indexPath = Path.of(Config.get("maven.index.path", "/index/"));
        int chunks = 0, rows = 0, saved = 0;
        byte[] propertiesBackup;
        try {
            Files.createDirectories(indexPath);
            propertiesBackup = readPropertiesBackup(indexPath);
        } catch (IOException e) {
            logger.warn("Cannot access local index directory [{}]", indexPath, e);
            return;
        }

        WritableResourceHandler local = new PathWritableResourceHandler(indexPath);
        ResourceHandler remote = new UriResourceHandler(Config.getURI("maven.repo-url").resolve("/maven2/.index/"));
        try (IndexReader indexReader = new IndexReader(local, remote)) {
            logger.info("Starting index scanning... indexId={}, incremental={}", indexReader.getIndexId(), indexReader.isIncremental());
            List<Gav> batch = new ArrayList<>(BATCH_SIZE);
            for (ChunkReader chunkReader : indexReader) {
                try (chunkReader) {
                    for (Map<String, String> row : chunkReader) {
                        rows++;
                        Gav gav = rowToGav(row);
                        if (gav == null) {
                            continue;
                        }
                        batch.add(gav);
                        if (batch.size() >= BATCH_SIZE) {
                            saved += repo.saveUnresolvedBatch(batch, UNRESOLVED_REASON);
                            batch.clear();
                            logger.info("Scanned {} rows, saved {}", rows, saved);
                        }
                    }
                }
                chunks++;
            }
            if (!batch.isEmpty()) {
                saved += repo.saveUnresolvedBatch(batch, UNRESOLVED_REASON);
            }
            Duration duration = Duration.between(start, Instant.now());
            logger.info("Scanning finished in {}s. chunks={}, rows={}, saved={}", duration.toSeconds(), chunks, rows, saved);
        } catch (Exception e) {
            Duration duration = Duration.between(start, Instant.now());
            logger.warn("Scanning failed in {}s. chunks={}, rows={}, saved={}", duration.toSeconds(), chunks, rows, saved, e);
            // IndexReader publishes local index properties on close even on failure,
            // which would make the next run skip the increment that was not fully processed
            restorePropertiesBackup(indexPath, propertiesBackup);
        }
    }

    private static byte[] readPropertiesBackup(Path indexPath) throws IOException {
        Path properties = indexPath.resolve(INDEX_PROPERTIES);
        return Files.exists(properties) ? Files.readAllBytes(properties) : null;
    }

    private static void restorePropertiesBackup(Path indexPath, byte[] backup) {
        Path properties = indexPath.resolve(INDEX_PROPERTIES);
        try {
            if (backup == null) {
                Files.deleteIfExists(properties);
            } else {
                Files.write(properties, backup);
            }
        } catch (IOException e) {
            logger.warn("Could not restore local index properties [{}]", properties, e);
        }
    }

    private static Gav rowToGav(Map<String, String> row) {
        String data = row.get("u");
        if (data == null) {
            return null;
        }
        String[] split = data.split("\\|");
        if (split.length > 3 && "NA".equals(split[3])) {
            return new Gav(split[0], split[1], split[2]);
        } else {
            // ignore all classifiers & metadata (hashes, signatures)
            return null;
        }
    }
}
