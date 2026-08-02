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
        checkIfClosed();
        return super.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkIfClosed();
        return super.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        checkIfClosed();
        return super.skip(n);
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            super.close();
        }
    }

    private void checkIfClosed() throws IOException {
        if (closed.get()) {
            throw new IOException("Stream already closed");
        }
    }
}
