package com.evolution.dropfile.common;

import java.io.IOException;

public final class ThrowableUtils {

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

        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return new IOException(throwable);
        }
        return new IOException(message, throwable);
    }

    public static RuntimeException rethrowRuntimeException(Throwable throwable) {
        if (throwable instanceof Error error) {
            throw error;
        }
        if (throwable instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }

        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return new RuntimeException(throwable);
        }
        return new RuntimeException(message, throwable);
    }
}
