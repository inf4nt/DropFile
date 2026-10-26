package com.evolution.dropfiledaemon.util;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.ThrowableUtils;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;

import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

@RequiredArgsConstructor
public class RetryExecutor<T> {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newVirtualThreadPerTaskExecutor();

    private final Callable<T> callable;

    private final BiConsumer<Integer, Exception> doOnError;

    private final BiConsumer<Integer, T> doOnSuccessful;

    private final Predicate<RetryIfContainer<T>> retryIf;

    private final int attempts;

    private final Duration delay;

    private final Duration callTimeout;

    public T run() throws ExecutionException, TimeoutException {
        List<Exception> exceptions = new ArrayList<>();
        int currentAttempt = 1;
        while (currentAttempt <= attempts) {
            try {
                CommonUtils.validateInterrupted();

                T call = callWithTimeoutIfPresent();

                boolean continueRetry = false;
                try {
                    continueRetry = retryIf.test(new RetryIfContainer<>(currentAttempt, call, null));
                } catch (Exception _) {
                }

                if (!continueRetry) {
                    if (doOnSuccessful != null) {
                        try {
                            doOnSuccessful.accept(currentAttempt, call);
                        } catch (Exception _) {
                        }
                    }
                    return call;
                }
            } catch (Exception e) {
                if (ThrowableUtils.checkThrowable(e, InterruptedException.class)) {
                    Thread.currentThread().interrupt();
                    rethrowToBasicExpectedExceptions(e);
                }
                if (ThrowableUtils.checkThrowable(e, ClosedChannelException.class)) {
                    rethrowToBasicExpectedExceptions(e);
                }

                boolean continueRetry = false;
                try {
                    continueRetry = retryIf.test(new RetryIfContainer<>(currentAttempt, null, e));
                } catch (Exception _) {
                }

                if (!continueRetry) {
                    rethrowToBasicExpectedExceptions(e);
                }

                exceptions.add(e);
                if (doOnError != null) {
                    try {
                        doOnError.accept(currentAttempt, e);
                    } catch (Exception _) {
                    }
                }
            }
            currentAttempt++;
            if (currentAttempt <= attempts && delay.isPositive()) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw ThrowableUtils.mapExceptionTo(
                            e,
                            it -> new IllegalStateException("Sleep retry was interrupted", it)
                    );
                }
            }
        }

        Exception exception = buildException(exceptions);
        if (exception != null) {
            rethrowToBasicExpectedExceptions(exception);
        }
        throw new IllegalStateException("Retry failed after " + attempts + " attempts with invalid result");
    }

    @Nullable
    private Exception buildException(List<Exception> exceptions) {
        if (exceptions.isEmpty()) {
            return null;
        }

        Exception first = exceptions.getFirst();

        for (int i = 1; i < exceptions.size(); i++) {
            Exception nested = exceptions.get(i);
            if (first != nested) {
                first.addSuppressed(nested);
            }
        }
        return first;
    }

    private T callWithTimeoutIfPresent() throws Exception {
        if (callTimeout == null || !callTimeout.isPositive()) {
            return callable.call();
        }

        Future<T> future = EXECUTOR_SERVICE.submit(callable);
        try {
            return future.get(callTimeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Error error) {
                throw error;
            }
            throw e;
        } catch (Exception e) {
            // Cancellation does not guarantee tack finished.
            // TODO add CountdownLatch to control it
            future.cancel(true);
            throw e;
        }
    }

    private void rethrowToBasicExpectedExceptions(Exception exception) throws ExecutionException, TimeoutException {
        if (exception instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (exception instanceof TimeoutException timeoutException) {
            throw timeoutException;
        }
        if (exception instanceof InterruptedException interruptedException) {
            throw ThrowableUtils.mapExceptionTo(
                    interruptedException,
                    it -> new IllegalStateException("Call was interrupted", it)
            );
        }
        if (exception instanceof ExecutionException executionException) {
            Throwable cause = executionException.getCause();
            if (cause instanceof Error error) {
                throw error;
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof TimeoutException timeoutException) {
                throw timeoutException;
            }
            if (cause instanceof InterruptedException interruptedException) {
                throw ThrowableUtils.mapExceptionTo(
                        interruptedException,
                        it -> new IllegalStateException("Call was interrupted", it)
                );
            }
            throw executionException;
        }
        throw ThrowableUtils.mapExceptionTo(
                exception,
                it -> new ExecutionException(it.getMessage(), it)
        );
    }

    public static <T> RetryExecutorBuilder<T> call(Callable<T> callable) {
        return new RetryExecutorBuilder<>(callable);
    }

    @RequiredArgsConstructor
    public static class RetryExecutorBuilder<T> {

        private final Callable<T> callable;

        private BiConsumer<Integer, Exception> doOnError;

        private BiConsumer<Integer, T> doOnSuccessful;

        private Predicate<RetryIfContainer<T>> retryIf = it -> it.result() == null || it.exception() != null;

        private int attempts = 48;

        private Duration delay = Duration.ofSeconds(5);

        private Duration callTimeout;

        public RetryExecutorBuilder<T> attempts(int attempts) {
            if (attempts <= 0) {
                throw new IllegalArgumentException("Attempt count must be positive");
            }
            this.attempts = attempts;
            return this;
        }

        public RetryExecutorBuilder<T> delay(Duration duration) {
            Objects.requireNonNull(duration, "Delay duration must not be null");
            this.delay = duration;
            return this;
        }

        public RetryExecutorBuilder<T> doOnError(BiConsumer<Integer, Exception> doOnError) {
            this.doOnError = Objects.requireNonNull(doOnError);
            return this;
        }

        public RetryExecutorBuilder<T> doOnSuccessful(BiConsumer<Integer, T> doOnSuccessful) {
            this.doOnSuccessful = Objects.requireNonNull(doOnSuccessful);
            return this;
        }

        public RetryExecutorBuilder<T> retryIf(Predicate<RetryIfContainer<T>> retryIf) {
            this.retryIf = Objects.requireNonNull(retryIf);
            return this;
        }

        public RetryExecutorBuilder<T> callTimeout(Duration duration) {
            Objects.requireNonNull(duration, "Call timeout duration must not be null");
            this.callTimeout = duration;
            return this;
        }

        public T run() throws ExecutionException, TimeoutException {
            RetryExecutor<T> retry = new RetryExecutor<>(
                    callable,
                    doOnError,
                    doOnSuccessful,
                    retryIf,
                    attempts,
                    delay,
                    callTimeout
            );
            return retry.run();
        }
    }

    public record RetryIfContainer<T>(Integer attempt, T result, Exception exception) {
    }
}
