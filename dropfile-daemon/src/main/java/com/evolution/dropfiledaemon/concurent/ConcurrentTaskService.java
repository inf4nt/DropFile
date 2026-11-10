package com.evolution.dropfiledaemon.concurent;

import com.evolution.dropfile.common.ThrowableUtils;
import com.evolution.dropfile.common.function.InterruptibleVoidCallable;
import com.evolution.dropfile.common.function.VoidCallable;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import jakarta.annotation.Nullable;
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

        List<InterruptedException> interruptedExceptions = awaitUninterruptibly(() -> countDownLatch.await());
        ExecutionException result = buildExecutionException(executionException, interruptedExceptions);
        if (result != null) {
            if (ThrowableUtils.checkThrowable(executionException, InterruptedException.class)) {
                Thread.currentThread().interrupt();
            }
            throw result;
        }
    }

    public <V> V invokeAny(Collection<Callable<V>> callables, Duration timeout) throws ExecutionException {
        Objects.requireNonNull(timeout, "timeout must not be null");

        if (!timeout.isPositive()) {
            throw new IllegalArgumentException("timeout must be positive");
        }

        if (CollectionUtils.isEmpty(callables)) {
            throw new IllegalArgumentException();
        }

        CountDownLatch countDownLatch = new CountDownLatch(callables.size());

        List<CallableCancelable<V>> callableCancelableList = callables.stream()
                .map(it -> new CallableCancelable<>(
                        it,
                        new CallableCancelable.SingleActionCountDownLatchCallableCancelableMiddleware(
                                countDownLatch
                        )
                ))
                .toList();

        V result = null;
        ExecutionException executionException = null;
        boolean interrupted = false;
        List<InterruptedException> interruptedExceptions;

        try {
            result = executorService.invokeAny(
                    callableCancelableList,
                    timeout.toNanos(),
                    TimeUnit.NANOSECONDS
            );
        } catch (ExecutionException e) {
            executionException = e;
        } catch (Throwable t) {
            interrupted = t instanceof InterruptedException;
            executionException = new ExecutionException(t.getMessage(), t);
        } finally {
            callableCancelableList.forEach(it -> it.cancel());
            interruptedExceptions = awaitUninterruptibly(() -> countDownLatch.await());
        }

        if (!interruptedExceptions.isEmpty()) {
            interrupted = true;

            if (executionException == null) {
                executionException = new ExecutionException(
                        "Operation was interrupted while waiting for tasks to finish",
                        interruptedExceptions.getFirst()
                );
            } else {
                executionException.addSuppressed(interruptedExceptions.getFirst());
            }
        }

        if (interrupted) {
            Thread.currentThread().interrupt();
        }

        if (executionException != null) {
            throw executionException;
        }

        return result;
    }

    public List<InterruptedException> awaitUninterruptibly(InterruptibleVoidCallable interruptibleVoidCallable) {
        List<InterruptedException> exceptions = new ArrayList<>();

        while (true) {
            try {
                interruptibleVoidCallable.call();
                return exceptions;
            } catch (InterruptedException e) {
                exceptions.add(e);
            }
        }
    }

    @Nullable
    private ExecutionException buildExecutionException(@Nullable ExecutionException executionException, List<? extends Throwable> suppressed) {
        ExecutionException result = executionException;
        List<Throwable> suppressedList = new ArrayList<>();
        if (result == null) {
            if (CollectionUtils.isEmpty(suppressed)) {
                return null;
            }
            Throwable first = suppressed.getFirst();
            suppressedList.addAll(suppressed.subList(1, suppressed.size()));
            result = new ExecutionException(first.getMessage(), first);
        } else {
            suppressedList.addAll(suppressed);
        }
        for (Throwable suppressedThrowable : suppressedList) {
            result.addSuppressed(suppressedThrowable);
        }
        return result;
    }
}
