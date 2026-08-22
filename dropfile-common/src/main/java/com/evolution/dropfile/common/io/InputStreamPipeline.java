package com.evolution.dropfile.common.io;

import com.evolution.dropfile.common.CommonUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class InputStreamPipeline implements AutoCloseable {

    private final AtomicBoolean closed = new AtomicBoolean();

    private final AtomicBoolean released = new AtomicBoolean();

    private InputStream current;

    private InputStreamPipeline(InputStream initial) {
        this.current = Objects.requireNonNull(initial);
    }

    public static InputStreamPipeline from(InputStream initial) {
        return new InputStreamPipeline(initial);
    }

    public InputStreamPipeline add(StreamWrapper wrapper) {
        if (released.get()) {
            throw new IllegalStateException("Already released");
        }

        try {
            InputStream next = wrapper.wrap(current);
            current = Objects.requireNonNull(next);
            return this;
        } catch (Throwable throwable) {
            try {
                close();
            } catch (Throwable closeThrowable) {
                throwable.addSuppressed(closeThrowable);
            }
            throw CommonUtils.toRuntimeException(throwable);
        }
    }

    public InputStream get() {
        if (released.compareAndSet(false, true)) {
            return current;
        }
        throw new IllegalStateException("Already released");
    }

    @Override
    public void close() throws IOException {
        if (released.get()) {
            return;
        }
        if (closed.compareAndSet(false, true)) {
            current.close();
        }
    }

    @FunctionalInterface
    public interface StreamWrapper {
        InputStream wrap(InputStream in) throws Exception;
    }
}
