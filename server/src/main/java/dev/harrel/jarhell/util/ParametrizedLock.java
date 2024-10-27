package dev.harrel.jarhell.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public final class ParametrizedLock<T> {
    private final ConcurrentMap<T, ReentrantLock> locks = new ConcurrentHashMap<>();

    public <R> R execute(T key, Supplier<R> supplier) {
        lock(key);
        try {
            return supplier.get();
        } finally {
            unlock(key);
        }
    }

    public void lock(T key) {
        locks.computeIfAbsent(key, k -> new ReentrantLock()).lock();
    }

    public void unlock(T key) {
        ReentrantLock lock = locks.get(key);
        if (lock != null) {
            lock.unlock();
        }
    }
}
