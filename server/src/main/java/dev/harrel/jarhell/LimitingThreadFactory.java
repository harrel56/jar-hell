package dev.harrel.jarhell;

import java.util.concurrent.*;

public class LimitingThreadFactory implements ThreadFactory {
    private final ThreadFactory factory;
    private final ExecutorService service;

    public LimitingThreadFactory(ThreadFactory factory, int limit) {
        this.factory = factory;
        this.service = Executors.newFixedThreadPool(limit, factory);
    }

    @Override
    public Thread newThread(Runnable r) {
        Runnable runnableWrapper = () -> {
            try {
                service.submit(r).get();
            } catch (ExecutionException e) {
                throw new CompletionException(e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CompletionException(e);
            }
        };
        return factory.newThread(runnableWrapper);
    }
}
