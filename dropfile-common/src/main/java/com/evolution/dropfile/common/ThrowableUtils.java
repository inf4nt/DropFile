package com.evolution.dropfile.common;

import java.io.IOException;

public final class ThrowableUtils {

    public static IOException rethrowIOException(Throwable throwable) throws IOException {
        if (throwable instanceof IOException ioException) {
            throw ioException;
        }
        if (throwable instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (throwable instanceof Error error) {
            throw error;
        }
        return new IOException(throwable.getMessage(), throwable);
    }

    public static RuntimeException rethrowRuntimeException(Throwable throwable) {
        if (throwable instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (throwable instanceof Error error) {
            throw error;
        }
        return new RuntimeException(throwable.getMessage(), throwable);
    }
}
