package com.evolution.dropfiledaemon.util;

import com.evolution.dropfiledaemon.util.function.IOConsumer;
import com.evolution.dropfiledaemon.util.function.IORunnable;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Function;

@AllArgsConstructor
public class Trying<T> {

    private final Callable<T> callable;

    private final IOConsumer<Exception> doOnError;

    private final IORunnable doFinally;

    public static <T> TryingBuilder<T> call(Callable<T> callable) {
        return new TryingBuilder<>(callable);
    }

    public static <T> TryingBuilder<T> call(IORunnable runnable) {
        return new TryingBuilder<>(() -> {
            runnable.run();
            return null;
        });
    }

    public T getOrElse(Function<? super Exception, ? extends T> orElse) {
        return get(Objects.requireNonNull(orElse), null);
    }

    public T getOrElseThrow() {
        return get(null, null);
    }

    public T getOrElseThrow(Function<? super Exception, ? extends Exception> orElseThrow) {
        return get(null, Objects.requireNonNull(orElseThrow));
    }

    @SneakyThrows
    private T get(Function<? super Exception, ? extends T> orElse,
                  Function<? super Exception, ? extends Exception> orElseThrow) {
        try {
            return callable.call();
        } catch (Exception e) {
            try {
                doOnError.accept(e);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            if (orElse != null) {
                return orElse.apply(e);
            }
            if (orElseThrow != null) {
                throw orElseThrow.apply(e);
            }
            throw e;
        } finally {
            if (doFinally != null) {
                try {
                    doFinally.run();
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }
    }

    @RequiredArgsConstructor
    public static class TryingBuilder<T> {

        private final Callable<T> callable;

        private IOConsumer<Exception> doOnError;

        private IORunnable doFinally;

        public TryingBuilder<T> doOnError(IOConsumer<Exception> doOnError) {
            this.doOnError = doOnError;
            return this;
        }

        public TryingBuilder<T> doFinally(IORunnable doFinally) {
            this.doFinally = doFinally;
            return this;
        }

        public Trying<T> build() {
            return new Trying<>(callable, doOnError, doFinally);
        }
    }
}
