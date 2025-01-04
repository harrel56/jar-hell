package dev.harrel.jarhell.util;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.StructuredTaskScope;

public final class ConcurrentUtil {
    public static void joinScope(StructuredTaskScope.ShutdownOnFailure scope) {
        try {
            scope.join().throwIfFailed(CompletionException::new);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompletionException(e);
        }
    }

    public static CompletableFuture<?> allOfFailFast(List<CompletableFuture<?>> futures) {
        CompletableFuture<Void> failure = new CompletableFuture<>();
        for (CompletableFuture<?> future: futures) {
            future.exceptionally(ex -> {
                failure.completeExceptionally(ex);
                throw new CompletionException(ex);
            });
        }
        failure.exceptionally(ex -> {
            for (CompletableFuture<?> future : futures) {
                future.cancel(true);
            }
            throw new CompletionException(ex);
        });
        return CompletableFuture.anyOf(failure, CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])));
    }
}
