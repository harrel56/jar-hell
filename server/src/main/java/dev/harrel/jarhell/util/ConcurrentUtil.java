package dev.harrel.jarhell.util;

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
}
