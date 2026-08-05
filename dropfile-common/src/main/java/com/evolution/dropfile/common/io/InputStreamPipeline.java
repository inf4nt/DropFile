package com.evolution.dropfile.common.io;

import com.evolution.dropfile.common.CommonUtils;

import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class InputStreamPipeline implements AutoCloseable {

    private final AtomicBoolean closed = new AtomicBoolean();

    private InputStream current;

    private boolean released = false;

    private InputStreamPipeline(InputStream initial) {
        this.current = Objects.requireNonNull(initial);
    }

    public static InputStreamPipeline from(InputStream initial) {
        return new InputStreamPipeline(initial);
    }

    public InputStreamPipeline add(StreamWrapper wrapper) {
        if (released || current == null) {
            return this;
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
        this.released = true;
        return current;
    }

    @Override
    public void close() throws Exception {
        if (!released && current != null) {
            if (closed.compareAndSet(false, true)) {
                try {
                    current.close();
                } catch (Throwable throwable) {
                    throw CommonUtils.toRuntimeException(throwable);
                }
            }
        }
    }

    @FunctionalInterface
    public interface StreamWrapper {
        InputStream wrap(InputStream in) throws Exception;
    }
}
