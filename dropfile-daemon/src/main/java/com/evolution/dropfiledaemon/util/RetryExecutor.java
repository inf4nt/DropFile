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
import java.util.function.Supplier;

@RequiredArgsConstructor
public class RetryExecutor<T> {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newVirtualThreadPerTaskExecutor();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> EXECUTOR_SERVICE.shutdownNow()));
    }

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
                if (e instanceof InterruptedException
                        || e.getCause() instanceof InterruptedException
                        || e instanceof ClosedChannelException
                        || e.getCause() instanceof ClosedChannelException) {
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
            Thread.sleep(delay.toMillis());
        }
        throw new RetryExecutorException(exceptions);
    }

    @SneakyThrows
    private T callWithTimeoutIfPresent() {
        if (callTimeout == null) {
            return callable.call();
        }

        try {
            CompletableFuture<T> future = CompletableFuture
                    .supplyAsync(
                            new Supplier<T>() {
                                @Override
                                @SneakyThrows
                                public T get() {
                                    return callable.call();
                                }
                            },
                            EXECUTOR_SERVICE
                    );
            long millis = callTimeout.toMillis();
            return future.get(millis, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            if (e instanceof ExecutionException executionException
                    && executionException.getCause() != null) {
                throw executionException.getCause();
            }
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
            this.doOnError = doOnError;
            return this;
        }

        public RetryExecutorBuilder<T> doOnSuccessful(BiConsumer<Integer, T> doOnSuccessful) {
            this.doOnSuccessful = doOnSuccessful;
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
