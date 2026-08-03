package com.evolution.dropfile.common.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class OnceCloseableInputStream extends FilterInputStream {

    private final AtomicBoolean closed = new AtomicBoolean();

    public OnceCloseableInputStream(InputStream in) {
        super(Objects.requireNonNull(in));
    }

    @Override
    public int read() throws IOException {
        ensureOpen();
        return super.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        ensureOpen();
        return super.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        ensureOpen();
        return super.skip(n);
    }

    @Override
    public int available() throws IOException {
        ensureOpen();
        return super.available();
    }

    @Override
    public void reset() throws IOException {
        ensureOpen();
        super.reset();
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            super.close();
        }
    }

    private void ensureOpen() throws IOException {
        if (closed.get()) {
            throw new IOException("Stream already closed");
        }
    }
}
