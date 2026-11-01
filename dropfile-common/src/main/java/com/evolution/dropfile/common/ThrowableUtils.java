package com.evolution.dropfile.common;

import java.io.IOException;
import java.util.*;

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

        return new IOException(throwable.getMessage(), throwable);
    }

    public static RuntimeException rethrowRuntimeException(Throwable throwable) {
        if (throwable instanceof Error error) {
            throw error;
        }
        if (throwable instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new RuntimeException(throwable.getMessage(), throwable);
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
