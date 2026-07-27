package com.evolution.dropfiledaemon.util;

import com.evolution.dropfile.common.CommonUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

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

    @SneakyThrows
    public T run() {
        List<Exception> exceptions = new ArrayList<>();
        int currentAttempt = 1;
        while (currentAttempt <= attempts) {
            try {
                CommonUtils.isInterrupted();

                T call = callWithTimeoutIfPresent();

                boolean continueRetry = retryIf.test(new RetryIfContainer<>(currentAttempt, call, null));
                if (!continueRetry) {
                    if (doOnSuccessful != null) {
                        try {
                            doOnSuccessful.accept(currentAttempt, call);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    return call;
                }
            } catch (Exception e) {
                if (CommonUtils.checkThrowable(e, InterruptedException.class)) {
                    if (!Thread.currentThread().isInterrupted()) {
                        Thread.currentThread().interrupt();
                    }
                    throw e;
                }
                if (CommonUtils.checkThrowable(e, ClosedChannelException.class)) {
                    throw e;
                }

                boolean continueRetry = retryIf.test(new RetryIfContainer<>(currentAttempt, null, e));
                if (!continueRetry) {
                    throw e;
                }

                exceptions.add(e);
                if (doOnError != null) {
                    try {
                        doOnError.accept(currentAttempt, e);
                    } catch (Exception doOnErrorException) {
                        doOnErrorException.printStackTrace();
                    }
                }
            }
            currentAttempt++;
            if (currentAttempt <= attempts) {
                try {
                    Thread.sleep(delay.toMillis());
                } catch (InterruptedException e) {
                    if (!Thread.currentThread().isInterrupted()) {
                        Thread.currentThread().interrupt();
                    }
                    throw e;
                }
            }
        }
        throw new RetryExecutorException(exceptions);
    }

    @SneakyThrows
    private T callWithTimeoutIfPresent() {
        if (callTimeout == null) {
            return callable.call();
        }

        Future<T> future = EXECUTOR_SERVICE.submit(callable);

        try {
            long millis = callTimeout.toMillis();
            return future.get(millis, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Exception exceptionCause) {
                throw exceptionCause;
            }
            throw e;
        } catch (Exception e) {
            future.cancel(true);
            throw e;
        }
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

        private int attempts = 15;

        private Duration delay = Duration.ofSeconds(5);

        private Duration callTimeout;

        public RetryExecutorBuilder<T> attempts(int attempts) {
            if (attempts <= 0) {
                throw new IllegalArgumentException("Attempt count must be positive");
            }
            this.attempts = attempts;
            return this;
        }

        public RetryExecutorBuilder<T> delay(Duration delay) {
            this.delay = Objects.requireNonNull(delay);
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
            this.callTimeout = Objects.requireNonNull(duration);
            return this;
        }

        public T run() {
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

    @RequiredArgsConstructor
    public static class RetryExecutorException extends RuntimeException {
        @Getter
        private final List<Exception> exceptions;
    }

    public record RetryIfContainer<T>(Integer attempt, T result, Exception exception) {
    }
}