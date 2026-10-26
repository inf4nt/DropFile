package com.evolution.dropfile.common;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

public class ThrowableUtils {

    public static IOException rethrowIOException(Throwable throwable) throws IOException {
        if (throwable instanceof Error error) {
            throw error;
        }
        if (throwable instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (throwable instanceof IOException ioException) {
            throw ioException;
        }

        return mapExceptionTo(throwable, it -> new IOException(it.getMessage(), it));
    }

    public static <T extends Throwable, R extends Throwable> R mapExceptionTo(T original, Function<T, R> mapFunction) {
        R newOne = mapFunction.apply(original);
        for (Throwable throwable : original.getSuppressed()) {
            newOne.addSuppressed(throwable);
        }
        return newOne;
    }

    public static RuntimeException rethrowRuntimeException(Throwable throwable) {
        if (throwable instanceof Error error) {
            throw error;
        }
        if (throwable instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        return mapExceptionTo(throwable, it -> new RuntimeException(it.getMessage(), it));
    }

    @SafeVarargs
    public static boolean checkThrowable(Throwable throwable,
                                         Class<? extends Throwable>... lookFor) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<Throwable> queue = new ArrayDeque<>();
        queue.add(throwable);

        while (!queue.isEmpty()) {
            Throwable current = queue.poll();

            if (current == null || !visited.add(current)) {
                continue;
            }

            for (Class<? extends Throwable> targetClass : lookFor) {
                if (targetClass.isInstance(current)) {
                    return true;
                }
            }

            if (current.getCause() != null) {
                queue.add(current.getCause());
            }

            Throwable[] suppressed = current.getSuppressed();
            if (suppressed != null) {
                for (Throwable sup : suppressed) {
                    if (sup != null) {
                        queue.add(sup);
                    }
                }
            }
        }

        return false;
    }
}
