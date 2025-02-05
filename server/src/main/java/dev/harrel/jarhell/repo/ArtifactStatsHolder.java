package dev.harrel.jarhell.repo;

import dev.harrel.jarhell.model.ArtifactInfo;
import org.eclipse.jetty.util.thread.AutoLock;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

class ArtifactStatsHolder {
    private static final int SIZE_LIMIT = 10;

    private final AutoLock lock = new AutoLock();
    private final ConcurrentLinkedDeque<ArtifactInfo> latest = new ConcurrentLinkedDeque<>();
    private final AtomicInteger analyzedCount = new AtomicInteger();

    void setLatestArtifacts(List<ArtifactInfo> artifacts) {
        try (AutoLock _ = lock.lock()) {
            latest.clear();
            latest.addAll(artifacts.subList(0, Math.min(SIZE_LIMIT, artifacts.size())));
        }
    }

    void setAnalyzedCount(int count) {
        analyzedCount.set(count);
    }

    void onArtifactSaved(ArtifactInfo artifact) {
        try (AutoLock _ = lock.lock()) {
            latest.addFirst(artifact);
            if (latest.size() > SIZE_LIMIT) {
                latest.removeLast();
            }
        }
        analyzedCount.incrementAndGet();
    }

    List<ArtifactInfo> getLatestArtifacts() {
        try (AutoLock _ = lock.lock()) {
            return List.copyOf(latest);
        }
    }

    int getAnalyzedCount() {
        return analyzedCount.get();
    }
}
