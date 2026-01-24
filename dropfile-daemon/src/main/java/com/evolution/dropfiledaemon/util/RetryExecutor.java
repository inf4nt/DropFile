package com.evolution.dropfiledaemon.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Function;

@AllArgsConstructor
public class RetryExecutor<T> {

    private final Callable<T> callable;

    private final Function<List<Exception>, T> fallbackMapper;

    private final BiConsumer<Integer, Exception> doOnError;

    private final BiConsumer<Integer, T> doOnSuccessful;

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
        if (fallbackMapper != null) {
            return fallbackMapper.apply(exceptions);
        }
        throw new RetryExecutorException(exceptions);
    }

    public static <T> RetryExecutorBuilder<T> call(Callable<T> callable) {
        return new RetryExecutorBuilder<>(callable);
    }

    @RequiredArgsConstructor
    public static class RetryExecutorBuilder<T> {

        private final Callable<T> callable;

        private Function<List<Exception>, T> fallbackMapper;

        private BiConsumer<Integer, Exception> doOnError;

        private BiConsumer<Integer, T> doOnSuccessful;

        private int attempts = 10;

        private Duration delay = Duration.ofSeconds(1);

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

        public RetryExecutorBuilder<T> fallbackMapper(Function<List<Exception>, T> fallbackMapper) {
            this.fallbackMapper = fallbackMapper;
            return this;
        }

        public RetryExecutor<T> build() {
            return new RetryExecutor<>(
                    callable,
                    fallbackMapper,
                    doOnError,
                    doOnSuccessful,
                    attempts,
                    delay
            );
        }
    }

    @RequiredArgsConstructor
    public static class RetryExecutorException extends RuntimeException {
        @Getter
        private final List<Exception> exceptions;
    }
}
