package dev.harrel.jarhell.repo;

import dev.harrel.jarhell.model.ArtifactInfo;
import org.eclipse.jetty.util.thread.AutoLock;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

class ArtifactStatsHolder {
    private static final int SIZE_LIMIT = 10;

    private final AutoLock lock = new AutoLock();
    private final ConcurrentLinkedDeque<ArtifactInfo> latest = new ConcurrentLinkedDeque<>();

    void setLatestArtifacts(List<ArtifactInfo> artifacts) {
        try (AutoLock _ = lock.lock()) {
            latest.clear();
            latest.addAll(artifacts.subList(0, Math.min(SIZE_LIMIT, artifacts.size())));
        }
    }

    void addLatestArtifact(ArtifactInfo artifact) {
        try (AutoLock _ = lock.lock()) {
            latest.addFirst(artifact);
            if (latest.size() > SIZE_LIMIT) {
                latest.removeLast();
            }
        }
    }

    List<ArtifactInfo> getLatestArtifacts() {
        try (AutoLock _ = lock.lock()) {
            return List.copyOf(latest);
        }
    }
}
