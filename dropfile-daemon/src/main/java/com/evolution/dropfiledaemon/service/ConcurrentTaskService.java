package com.evolution.dropfiledaemon.service;

import com.evolution.dropfile.common.CommonUtils;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ConcurrentTaskService {

    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    @EventListener(ContextClosedEvent.class)
    public void close() {
        executorService.shutdownNow();
    }

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

    public void invokeAny(List<Callable<Void>> callables, Duration timeout) throws ExecutionException {
        Objects.requireNonNull(timeout, "timeout must not be null");
        if (!timeout.isPositive()) {
            throw new IllegalArgumentException("timeout must be positive");
        }

        if (CollectionUtils.isEmpty(callables)) {
            return;
        }

        try {
            executorService.invokeAny(callables, timeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecutionException(e.getMessage(), e);
        } catch (TimeoutException e) {
            throw new ExecutionException(e.getMessage(), e);
        }
    }
}
