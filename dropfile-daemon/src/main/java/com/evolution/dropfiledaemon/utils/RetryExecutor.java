package com.evolution.dropfiledaemon.utils;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Function;

@AllArgsConstructor
public class RetryExecutor<T> {

    private final Callable<T> callable;

    private final Function<List<Exception>, ? extends Exception> exceptionMapper;

    private final Function<List<Exception>, T> fallbackMapper;

    private final BiConsumer<Integer, Exception> retryNotification;

    private final int attempts;

    private final Duration delay;

    @SneakyThrows
    public T run() {
        List<Exception> exceptions = new ArrayList<>();
        int currentAttempt = 1;
        while (currentAttempt <= attempts) {
            try {
                T call = callable.call();
                if (call != null) {
                    return call;
                }
                throw new NullPointerException(String.format(
                        "Call returned null. Attempt %s", currentAttempt
                ));
            } catch (Exception e) {
                exceptions.add(e);
                if (retryNotification != null) {
                    retryNotification.accept(currentAttempt, e);
                }
            }
            currentAttempt++;
            Thread.sleep(delay.toMillis());
        }
        if (exceptionMapper != null) {
            throw exceptionMapper.apply(exceptions);
        }
        if (fallbackMapper != null) {
            return fallbackMapper.apply(exceptions);
        }
        throw new RuntimeException();
    }

    public static <T> RetryExecutorBuilder<T> call(Callable<T> callable) {
        return new RetryExecutorBuilder<>(callable);
    }

    public static class RetryExecutorBuilder<T> {

        private final Callable<T> callable;

        private Function<List<Exception>, ? extends Exception> exceptionMapper;

        private Function<List<Exception>, T> fallbackMapper;

        private BiConsumer<Integer, Exception> retryNotification;

        private int attempts = 10;

        private Duration delay = Duration.ofSeconds(1);

        public RetryExecutorBuilder(Callable<T> callable) {
            this.callable = callable;
        }

        public RetryExecutorBuilder<T> attempts(int attempts) {
            this.attempts = attempts;
            return this;
        }

        public RetryExecutorBuilder<T> delay(Duration delay) {
            this.delay = delay;
            return this;
        }

        public RetryExecutorBuilder<T> doOnError(BiConsumer<Integer, Exception> retryNotification) {
            this.retryNotification = retryNotification;
            return this;
        }

        public RetryExecutorBuilder<T> exceptionMapper(Function<List<Exception>, ? extends Exception> exceptionMapper) {
            if (fallbackMapper != null) {
                throw new RuntimeException("exceptionMapper already set");
            }
            this.exceptionMapper = exceptionMapper;
            return this;
        }

        public RetryExecutorBuilder<T> fallbackMapper(Function<List<Exception>, T> fallbackMapper) {
            if (exceptionMapper != null) {
                throw new RuntimeException("exceptionMapper already set");
            }
            this.fallbackMapper = fallbackMapper;
            return this;
        }

        public RetryExecutor<T> build() {
            return new RetryExecutor<>(
                    callable,
                    exceptionMapper,
                    fallbackMapper,
                    retryNotification,
                    attempts,
                    delay
            );
        }
    }
}
