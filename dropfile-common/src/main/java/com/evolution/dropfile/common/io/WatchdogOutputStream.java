package com.evolution.dropfile.common.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public class WatchdogOutputStream extends FilterOutputStream {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newVirtualThreadPerTaskExecutor();

    private final AtomicReference<Boolean> closing = new AtomicReference<>(false);

    private final AtomicReference<Boolean> closed = new AtomicReference<>(false);

    private volatile boolean timedOut = false;

    final Future<?> watchdogTask;

    public WatchdogOutputStream(OutputStream out, Duration duration) {
        super(Objects.requireNonNull(out, "OutputStream cannot be null"));

        if (duration != null && duration.isPositive()) {
            this.watchdogTask = EXECUTOR_SERVICE.submit(() -> {
                try {
                    Thread.sleep(duration.toMillis());
                    safeClose();
                } catch (InterruptedException _) {
                    Thread.currentThread().interrupt();
                }
            });
        } else {
            this.watchdogTask = null;
        }
    }

    private void safeClose() {
        timedOut = true;
        closed.set(true);
        try {
            out.close();
        } catch (Exception _) {
        }
    }

    private void cancelWatchdog() {
        if (watchdogTask != null) {
            watchdogTask.cancel(true);
        }
    }

    private void ensureOpen() throws IOException {
        if (closing.get() || timedOut) {
            throw new IOException("Stream closed");
        }
    }

    @Override
    public void write(int b) throws IOException {
        ensureOpen();
        out.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        ensureOpen();
        out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        if (closed.get()) {
            throw new IOException("Already closed");
        }
        out.flush();
    }

    @Override
    public void close() throws IOException {
        if (closed.get()) {
            return;
        }

        if (closing.compareAndSet(false, true)) {
            try {
                cancelWatchdog();
                super.close();
            } finally {
                closed.set(true);
            }
        }
    }
}
