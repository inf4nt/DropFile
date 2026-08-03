package com.evolution.dropfile.common.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CloseShieldOutputStream extends FilterOutputStream {

    private final AtomicBoolean closed = new AtomicBoolean(false);

    private CloseShieldOutputStream(OutputStream out) {
        super(out);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            out.flush();
        }
    }

    public static CloseShieldOutputStream stream(OutputStream outputStream) {
        Objects.requireNonNull(outputStream);

        if (outputStream instanceof CloseShieldOutputStream closeShieldOutputStream) {
            return closeShieldOutputStream;
        }
        return new CloseShieldOutputStream(outputStream);
    }
}
