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

    public T run() throws ExecutionException {
        List<Exception> exceptions = new ArrayList<>();
        int currentAttempt = 1;
        while (currentAttempt <= attempts) {
            try {
                CommonUtils.validateInterrupted();

                T call = callWithTimeoutIfPresent();

                boolean continueRetry = false;
                try {
                    continueRetry = retryIf.test(new RetryIfContainer<>(currentAttempt, call, null));
                } catch (Throwable _) {
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
                if (ThrowableUtils.checkThrowable(e, Error.class)) {
                    throw toBasicExpectedExceptions(e);
                }
                if (ThrowableUtils.checkThrowable(e, InterruptedException.class)) {
                    Thread.currentThread().interrupt();
                    throw toBasicExpectedExceptions(e);
                }
                if (ThrowableUtils.checkThrowable(e, ClosedChannelException.class)) {
                    throw toBasicExpectedExceptions(e);
                }

                boolean continueRetry = false;
                try {
                    continueRetry = retryIf.test(new RetryIfContainer<>(currentAttempt, null, e));
                } catch (Throwable _) {
                }

                if (!continueRetry) {
                    throw toBasicExpectedExceptions(e);
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
                    throw toBasicExpectedExceptions(e);
                }
            }
        }

        Exception exception = buildException(exceptions);
        if (exception != null) {
            throw toBasicExpectedExceptions(exception);
        }

        IllegalStateException illegalStateException = new IllegalStateException(
                "Retry failed after " + attempts + " attempts with invalid result"
        );
        throw new ExecutionException(illegalStateException.getMessage(), illegalStateException);
    }

    @Nullable
    private Exception buildException(List<Exception> exceptions) {
        if (exceptions.isEmpty()) {
            return null;
        }

        Exception first = exceptions.getFirst();
        for (int i = 1; i < exceptions.size(); i++) {
            Throwable nested = exceptions.get(i);
            if (nested instanceof ExecutionException && nested.getCause() != null) {
                nested = nested.getCause();
            }
            if (first != nested && first.getCause() != nested) {
                first.addSuppressed(nested);
            }
        }
        return first;
    }

    private T callWithTimeoutIfPresent() throws ExecutionException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        CallableCancelable<T> callableCancelable = new CallableCancelable<>(
                callable,
                new CallableCancelable.SingleActionCountDownLatchCallableCancelableMiddleware(
                        countDownLatch
                )
        );

        Future<T> future;
        try {
            future = EXECUTOR_SERVICE.submit(callableCancelable);
        } catch (Throwable throwable) {
            ExecutionException failure = new ExecutionException(throwable.getMessage(), throwable);
            callableCancelable.cancel();
            awaitUninterruptibly(countDownLatch, failure);
            throw failure;
        }

        try {
            if (callTimeout == null || !callTimeout.isPositive()) {
                return future.get();
            }
            return future.get(callTimeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (ExecutionException e) {
            throw e;
        } catch (Throwable throwable) {
            ExecutionException failure = new ExecutionException(throwable.getMessage(), throwable);
            future.cancel(true);
            callableCancelable.cancel();

            awaitUninterruptibly(countDownLatch, failure);

            if (throwable instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            throw failure;
        }
    }

    private void awaitUninterruptibly(CountDownLatch latch, Throwable primaryException) {
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    latch.await();
                    return;
                } catch (InterruptedException e) {
                    interrupted = true;
                    primaryException.addSuppressed(e);
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private ExecutionException toBasicExpectedExceptions(Exception exception) {
        if (exception instanceof ExecutionException executionException) {
            return executionException;
        }
        return new ExecutionException(exception.getMessage(), exception);
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

        public T run() throws ExecutionException {
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
