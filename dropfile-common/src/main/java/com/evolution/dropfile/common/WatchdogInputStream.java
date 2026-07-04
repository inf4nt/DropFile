package com.evolution.dropfile.common;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class WatchdogInputStream extends InputStreamDecorator {

    private static final ExecutorService WATCHDOG_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final long limit;

    final Future<?> watchdogTask;

    private long bytesRead;

    private final AtomicBoolean closed = new AtomicBoolean(false);

    public WatchdogInputStream(InputStream in) {
        this(in, Long.MAX_VALUE, null);
    }

    public WatchdogInputStream(InputStream in, long limit) {
        this(in, limit, null);
    }

    public WatchdogInputStream(InputStream in, long limit, Duration duration) {
        super(in);
        this.limit = limit;

        if (duration != null) {
            this.watchdogTask = WATCHDOG_EXECUTOR.submit(() -> {
                Thread.sleep(duration);
                close();
                return null;
            });
        } else {
            this.watchdogTask = null;
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read() throws IOException {
        ensureOpen();

        if (isLimitReached()) {
            return -1;
        }

        int b = super.read();

        if (b == -1) {
            finalizeStream();
            return -1;
        }

        bytesRead++;
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        ensureOpen();

        if (isLimitReached()) {
            return -1;
        }

        int maxToRead = (limit > 0) ? (int) Math.min(len, limit - bytesRead) : len;

        int result = super.read(b, off, maxToRead);

        if (result == -1) {
            finalizeStream();
            return -1;
        }

        bytesRead += result;
        if (isLimitReached()) {
            finalizeStream();
        }
        return result;
    }

    private boolean isLimitReached() {
        return limit > 0 && bytesRead >= limit;
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            finalizeStream();
            super.close();
        }
    }

    private void finalizeStream() {
        if (watchdogTask != null && !watchdogTask.isDone()) {
            // TODO test cancel(false) vs .cancel(true)
            watchdogTask.cancel(false);
        }
    }

    private void ensureOpen() throws IOException {
        if (closed.get()) {
            throw new IOException("Stream already closed");
        }
    }
}
