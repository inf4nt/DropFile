package com.evolution.dropfile.common;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
        executeWithGlobalLock(() -> {
            keyLocks.keySet().removeIf(purgePredicate::test);
        });
    }

    public <R, TH extends Throwable> R executeWithKeyLock(String key, SupplierThrowable<R, TH> action) throws TH {
        acquireLock(globalLock.readLock(), "global read lock");
        try {
            Lock keyLock = keyLocks.computeIfAbsent(key, _ -> new ReentrantLock());

            acquireLock(keyLock, "key lock [" + key + "]");
            try {
                return action.get();
            } finally {
                keyLock.unlock();
            }
        } finally {
            globalLock.readLock().unlock();
        }
    }

    public <TH extends Throwable> void executeWithKeyLock(String key, ProcedureThrowable<TH> action) throws TH {
        executeWithKeyLock(key, () -> {
            action.execute();
            return null;
        });
    }

    public <R, TH extends Throwable> R executeWithGlobalLock(SupplierThrowable<R, TH> action) throws TH {
        acquireLock(globalLock.writeLock(), "global write lock");
        try {
            return action.get();
        } finally {
            globalLock.writeLock().unlock();
        }
    }

    public <TH extends Throwable> void executeWithGlobalLock(ProcedureThrowable<TH> action) throws TH {
        executeWithGlobalLock(() -> {
            action.execute();
            return null;
        });
    }

    @SneakyThrows
    private void acquireLock(Lock lock, String lockName) {
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

    public interface SupplierThrowable<R, TH extends Throwable> {
        R get() throws TH;
    }

    public interface ProcedureThrowable<TH extends Throwable> {
        void execute() throws TH;
    }
}
