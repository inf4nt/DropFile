package com.evolution.dropfiledaemon.service;

import com.evolution.dropfile.common.function.VoidCallable;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.util.CallableCancelable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ConcurrentTaskService {

    private final ExecutorService executorService;

    private final ExecutorService downloadProcedureExecutorService;

    @Autowired
    public ConcurrentTaskService(DaemonApplicationProperties daemonApplicationProperties) {
        this(daemonApplicationProperties.daemonDownloadProcedureThreadSize);
    }

    public ConcurrentTaskService(int downloadProcedureThreadSize) {
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.downloadProcedureExecutorService = Executors.newFixedThreadPool(downloadProcedureThreadSize, Thread.ofVirtual().factory());
    }

    @EventListener(ContextClosedEvent.class)
    public void close() {
        executorService.shutdownNow();
        downloadProcedureExecutorService.shutdownNow();
    }

    public void executeFailFastDownloadProcedure(Collection<VoidCallable> callables) throws ExecutionException {
        if (CollectionUtils.isEmpty(callables)) {
            return;
        }

        CountDownLatch countDownLatch = new CountDownLatch(callables.size());

        List<CallableCancelable<Void>> callableCancelableList = callables.stream()
                .map(it -> new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        it.call();
                        return null;
                    }
                })
                .map(it -> new CallableCancelable<>(
                        it,
                        new CallableCancelable.SingleActionCountDownLatchCallableCancelableMiddleware(
                                countDownLatch
                        )
                ))
                .toList();

        CompletionService<Void> completionService = new ExecutorCompletionService<>(downloadProcedureExecutorService);

        List<Future<Void>> futures = new ArrayList<>(callables.size());
        AtomicReference<Throwable> throwableAtomicReference = new AtomicReference<>();

        for (CallableCancelable<Void> callable : callableCancelableList) {
            Future<Void> future;
            try {
                future = completionService.submit(callable);
            } catch (RuntimeException e) {
                throwableAtomicReference.compareAndSet(null, e);
                break;
            }
            futures.add(future);
        }

        if (throwableAtomicReference.get() == null) {
            for (int i = 0; i < futures.size(); i++) {
                try {
                    completionService.take().get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throwableAtomicReference.compareAndSet(null, e);
                    break;
                } catch (CancellationException _) {

                } catch (ExecutionException e) {
                    throwableAtomicReference.compareAndSet(null, e);
                    break;
                }
            }
        }

        try {
            Throwable throwable = throwableAtomicReference.get();
            if (throwable != null) {
                futures.forEach(it -> it.cancel(true));
                callableCancelableList.forEach(it -> it.cancel());

                if (throwable instanceof ExecutionException executionException) {
                    Throwable cause = executionException.getCause();
                    if (cause instanceof Error error) {
                        throwableAtomicReference.set(error);
                        throw error;
                    }
                    if (cause instanceof RuntimeException runtimeException) {
                        throwableAtomicReference.set(runtimeException);
                        throw runtimeException;
                    }
                    throw executionException;
                }
                if (throwable instanceof RuntimeException runtimeException) {
                    throwableAtomicReference.set(runtimeException);
                    throw runtimeException;
                }
                ExecutionException executionException = new ExecutionException(throwable.getMessage(), throwable);
                throwableAtomicReference.set(executionException);
                throw executionException;
            }
        } finally {
            awaitUninterruptibly(countDownLatch, throwableAtomicReference.get());
        }
    }

    public void invokeAny(List<Callable<Void>> callables, Duration timeout) throws ExecutionException, TimeoutException {
        Objects.requireNonNull(timeout, "timeout must not be null");
        if (!timeout.isPositive()) {
            throw new IllegalArgumentException("timeout must be positive");
        }

        if (CollectionUtils.isEmpty(callables)) {
            return;
        }

        // TODO and await
        try {
            executorService.invokeAny(callables, timeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Call was interrupted", e);
        }
    }

    private static void awaitUninterruptibly(
            CountDownLatch latch,
            Throwable primaryThrowable
    ) {
        boolean interrupted = false;

        try {
            while (true) {
                try {
                    latch.await();
                    return;
                } catch (InterruptedException e) {
                    interrupted = true;
                    primaryThrowable.addSuppressed(e);
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
