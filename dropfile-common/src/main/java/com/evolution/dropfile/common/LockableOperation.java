package com.evolution.dropfile.common;

import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

@RequiredArgsConstructor
public class LockableOperation implements Purgeable {

    private static final long LOCK_TIMEOUT_SECONDS = 30;

    private final ReadWriteLock globalLock = new ReentrantReadWriteLock();

    private final Map<String, Lock> keyLocks = new ConcurrentHashMap<>();

    private final Predicate<String> purgePredicate;

    @Override
    public void purge() {
        try {
            executeWithGlobalLock(() -> {
                keyLocks.keySet().removeIf(it -> purgePredicate.test(it));
            });
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <R> R executeWithKeyLock(String key, Callable<R> callable)
            throws InterruptedException, TimeoutException, ExecutionException {
        acquireLock(globalLock.readLock(), "global read lock");
        try {
            Lock keyLock = keyLocks.computeIfAbsent(key, _ -> new ReentrantLock());

            acquireLock(keyLock, "key lock [" + key + "]");
            try {
                return call(callable);
            } finally {
                keyLock.unlock();
            }
        } finally {
            globalLock.readLock().unlock();
        }
    }

    public <R> R executeWithGlobalLock(Callable<R> callable) throws InterruptedException, TimeoutException, ExecutionException {
        acquireLock(globalLock.writeLock(), "global write lock");
        try {
            return call(callable);
        } finally {
            globalLock.writeLock().unlock();
        }
    }

    public void executeWithGlobalLock(Runnable runnable) throws InterruptedException, TimeoutException, ExecutionException {
        executeWithGlobalLock(() -> {
            runnable.run();
            return null;
        });
    }

    private void acquireLock(Lock lock, String lockName) throws InterruptedException, TimeoutException {
        boolean acquired;
        try {
            acquired = lock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }

        if (!acquired) {
            throw new TimeoutException("Failed to acquire " + lockName + " within " + LOCK_TIMEOUT_SECONDS + " seconds");
        }
    }

    private <R> R call(Callable<R> callable) throws ExecutionException, InterruptedException, TimeoutException {
        try {
            return callable.call();
        } catch (RuntimeException | TimeoutException | ExecutionException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (Exception e) {
            String message = e.getMessage();
            if (message == null || message.isBlank()) {
                throw new ExecutionException(e);
            }
            throw new ExecutionException(message, e);
        }
    }
}
