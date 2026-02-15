package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.model.ArtifactInfo;
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
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Singleton
public class MavenIndexService {
    private static final Logger logger = LoggerFactory.getLogger(MavenIndexService.class);

    private final ArtifactRepository repo;

    MavenIndexService(ArtifactRepository repo) {
        this.repo = repo;
    }

    public void scanIndex() {
        Instant start = Instant.now();
        WritableResourceHandler local = new PathWritableResourceHandler(Path.of("/index/"));
        ResourceHandler remote = new UriResourceHandler(Config.getURI("maven.repo-url").resolve("/maven2/.index/"));
        int chunks = 0, rows = 0, saved = 0;
        logger.info("Starting index scanning...");
        try (IndexReader indexReader = new IndexReader(local, remote)) {
            for (ChunkReader chunkReader : indexReader) {
                try (chunkReader) {
                    for (Map<String, String> row : chunkReader) {
                        Gav gav = rowToGav(row);
                        if (gav != null && !repo.exists(gav)) {
                            repo.saveArtifact(ArtifactInfo.unresolved(gav, "initial-indexing"));
                            saved++;
                        }
                        rows++;
                        if (rows % 100_000 == 0) {
                            logger.info("Scanned {} rows, saved {}", rows, saved);
                        }
                    }
                }
                chunks++;
            }
            Duration duration = Duration.between(start, Instant.now());
            logger.info("Scanning finished in {}s. chunks={}, rows={}, saved={}", duration.toSeconds(), chunks, rows, saved);
        } catch (Exception e) {
            Duration duration = Duration.between(start, Instant.now());
            logger.warn("Scanning failed in {}s. chunks={}, rows={}, saved={}", duration.toSeconds(), chunks, rows, saved, e);
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
