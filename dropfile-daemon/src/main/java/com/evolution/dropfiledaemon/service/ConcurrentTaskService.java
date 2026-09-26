package com.evolution.dropfiledaemon.service;

import com.evolution.dropfile.common.CommonUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ConcurrentTaskService {

    public void executeFailFast(List<Callable<Void>> callables,
                                int threadCount,
                                ExecutorService executorService) throws ExecutionException {
        if (threadCount <= 0) {
            throw new IllegalArgumentException("Thread count must be greater than zero");
        }
        if (CollectionUtils.isEmpty(callables)) {
            return;
        }

        AtomicReference<Throwable> firstThrowable = new AtomicReference<>();
        List<Future<?>> activeFutures = new CopyOnWriteArrayList<>();
        Semaphore semaphore = new Semaphore(threadCount);

        for (Callable<Void> callable : callables) {
            if (firstThrowable.get() != null) {
                break;
            }

            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                firstThrowable.compareAndSet(null, e);
                break;
            }

            Future<?> future;
            try {
                future = executorService.submit(() -> {
                    try {
                        if (firstThrowable.get() != null) {
                            return;
                        }
                        callable.call();
                    } catch (Throwable t) {
                        if (firstThrowable.compareAndSet(null, t)) {
                            activeFutures.forEach(it -> it.cancel(true));
                        }
                    } finally {
                        semaphore.release();
                    }
                });
            } catch (Throwable t) {
                semaphore.release();
                if (firstThrowable.compareAndSet(null, t)) {
                    activeFutures.forEach(it -> it.cancel(true));
                }
                break;
            }

            activeFutures.add(future);
        }

        for (Future<?> future : activeFutures) {
            try {
                future.get();
            } catch (CancellationException _) {

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                firstThrowable.compareAndSet(null, e);
            } catch (ExecutionException e) {
                firstThrowable.compareAndSet(null, e.getCause());
            }
        }

        Throwable throwable = firstThrowable.get();
        if (throwable != null) {
            if (CommonUtils.checkThrowable(throwable, InterruptedException.class)) {
                Thread.currentThread().interrupt();
            }

            if (throwable instanceof Error error) {
                throw error;
            }

            throw new ExecutionException(throwable.getMessage(), throwable);
        }
    }
}
