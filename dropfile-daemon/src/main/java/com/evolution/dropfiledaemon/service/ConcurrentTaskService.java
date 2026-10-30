package com.evolution.dropfiledaemon.service;

import com.evolution.dropfile.common.ThrowableUtils;
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
                .map(it -> it.toCallable())
                .map(it -> new CallableCancelable<>(
                        it,
                        new CallableCancelable.SingleActionCountDownLatchCallableCancelableMiddleware(
                                countDownLatch
                        )
                ))
                .toList();

        CompletionService<Void> completionService = new ExecutorCompletionService<>(downloadProcedureExecutorService);

        List<Future<Void>> futures = new ArrayList<>(callables.size());
        ExecutionException executionException = null;

        for (CallableCancelable<Void> callable : callableCancelableList) {
            Future<Void> future;
            try {
                future = completionService.submit(callable);
            } catch (RuntimeException e) {
                executionException = new ExecutionException(e.getMessage(), e);
                break;
            }
            futures.add(future);
        }

        if (executionException == null) {
            for (int i = 0; i < futures.size(); i++) {
                try {
                    completionService.take().get();
                } catch (InterruptedException e) {
                    executionException = new ExecutionException(e.getMessage(), e);
                    break;
                } catch (ExecutionException e) {
                    executionException = e;
                    break;
                } catch (CancellationException _) {

                }
            }
        }

        if (executionException != null) {
            futures.forEach(it -> it.cancel(true));
            callableCancelableList.forEach(it -> it.cancel());
        }

        List<InterruptedException> interruptedExceptions = awaitUninterruptibly(countDownLatch);

        if (executionException != null) {
            for (InterruptedException interruptedException : interruptedExceptions) {
                executionException.addSuppressed(interruptedException);
            }
            if (ThrowableUtils.checkThrowable(executionException, InterruptedException.class)) {
                Thread.currentThread().interrupt();
            }
            throw executionException;
        }

        if (!interruptedExceptions.isEmpty()) {
            InterruptedException first = interruptedExceptions.getFirst();
            executionException = new ExecutionException(first.getMessage(), first);
            for (int i = 1; i < interruptedExceptions.size(); i++) {
                executionException.addSuppressed(interruptedExceptions.get(i));
            }
            Thread.currentThread().interrupt();
            throw executionException;
        }
    }

    private List<InterruptedException> awaitUninterruptibly(CountDownLatch latch) {
        List<InterruptedException> exceptions = new ArrayList<>();

        while (true) {
            try {
                latch.await();
                return exceptions;
            } catch (InterruptedException e) {
                exceptions.add(e);
            }
        }
    }

    public void invokeAny(List<VoidCallable> callables, Duration timeout) throws ExecutionException {
        Objects.requireNonNull(timeout, "timeout must not be null");
        if (!timeout.isPositive()) {
            throw new IllegalArgumentException("timeout must be positive");
        }

        if (CollectionUtils.isEmpty(callables)) {
            return;
        }

        CountDownLatch countDownLatch = new CountDownLatch(callables.size());

        List<CallableCancelable<Void>> callableCancelableList = callables.stream()
                .map(it -> it.toCallable())
                .map(it -> new CallableCancelable<>(
                        it,
                        new CallableCancelable.SingleActionCountDownLatchCallableCancelableMiddleware(
                                countDownLatch
                        )
                ))
                .toList();

        ExecutionException executionException = null;

        try {
            executorService.invokeAny(callableCancelableList, timeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (ExecutionException e) {
            executionException = e;
        } catch (Throwable throwable) {
            executionException = new ExecutionException(throwable.getMessage(), throwable);
        } finally {
            callableCancelableList.forEach(CallableCancelable::cancel);
        }

        List<InterruptedException> interruptedExceptions = awaitUninterruptibly(countDownLatch);

        if (!interruptedExceptions.isEmpty()) {
            if (executionException == null) {
                InterruptedException first = interruptedExceptions.getFirst();
                executionException = new ExecutionException(first.getMessage(), first);

                for (int i = 1; i < interruptedExceptions.size(); i++) {
                    executionException.addSuppressed(interruptedExceptions.get(i));
                }
            } else {
                for (InterruptedException interruptedException : interruptedExceptions) {
                    executionException.addSuppressed(interruptedException);
                }
            }
        }

        if (executionException != null) {
            if (ThrowableUtils.checkThrowable(executionException, InterruptedException.class)) {
                Thread.currentThread().interrupt();
            }

            throw executionException;
        }
    }
}
