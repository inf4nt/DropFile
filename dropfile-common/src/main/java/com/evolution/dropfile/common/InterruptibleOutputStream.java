package com.evolution.dropfile.common;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class InterruptibleOutputStream extends FilterOutputStream {

    private final AtomicBoolean aborted = new AtomicBoolean(false);

    private InterruptibleOutputStream(OutputStream out) {
        super(Objects.requireNonNull(out));
    }

    @Override
    public void write(int b) throws IOException {
        checkInterrupted();
        out.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        checkInterrupted();
        out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        checkInterrupted();
        out.flush();
    }

    @Override
    public void close() throws IOException {
        checkInterrupted();
        out.flush();
    }

    private void checkInterrupted() throws IOException {
        if (aborted.get() || Thread.currentThread().isInterrupted()) {
            aborted.set(true);
            throw new IOException(new InterruptedException("Stream execution aborted: thread was interrupted"));
        }
    }

    public static InterruptibleOutputStream stream(OutputStream outputStream) {
        if (outputStream instanceof InterruptibleOutputStream existing) {
            return existing;
        }
        return new InterruptibleOutputStream(outputStream);
    }
}
