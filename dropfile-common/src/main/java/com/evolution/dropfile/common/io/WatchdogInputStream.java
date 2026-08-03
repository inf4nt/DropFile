package com.evolution.dropfile.common.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class WatchdogInputStream extends FilterInputStream {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newVirtualThreadPerTaskExecutor();

    private final long limit;

    final Future<?> watchdogTask;

    private long bytesRead;

    private final AtomicBoolean closed = new AtomicBoolean(false);

    public WatchdogInputStream(InputStream in, long limit) {
        this(in, limit, null);
    }

    public WatchdogInputStream(InputStream in, long limit, Duration duration) {
        super(Objects.requireNonNull(in, "InputStream cannot be null"));

        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }

        this.limit = limit;

        if (duration == null) {
            this.watchdogTask = null;
            return;
        }

        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("Duration must be positive");
        }

        this.watchdogTask = EXECUTOR_SERVICE.submit(() -> {
            try {
                Thread.sleep(duration.toMillis());
                safeClose();
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }
        });
    }

    @Override
    public int read() throws IOException {
        ensureOpen();

        if (isLimitReached()) {
            return -1;
        }

        int b = super.read();

        if (b == -1) {
            cancelWatchdog();
            return -1;
        }

        bytesRead++;

        if (isLimitReached()) {
            cancelWatchdog();
        }

        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);

        ensureOpen();

        if (len == 0) {
            return 0;
        }

        if (isLimitReached()) {
            return -1;
        }

        int maxToRead = (int) Math.min(len, limit - bytesRead);

        int result = super.read(b, off, maxToRead);

        if (result == -1) {
            cancelWatchdog();
            return -1;
        }

        bytesRead += result;

        if (isLimitReached()) {
            cancelWatchdog();
        }

        return result;
    }

    @Override
    public long skip(long n) throws IOException {
        ensureOpen();

        if (n <= 0) {
            return 0;
        }

        if (isLimitReached()) {
            return 0;
        }

        long maxToSkip = Math.min(n, limit - bytesRead);
        long skipped = super.skip(maxToSkip);

        bytesRead += skipped;

        if (isLimitReached()) {
            cancelWatchdog();
        }

        return skipped;
    }

    @Override
    public int available() throws IOException {
        ensureOpen();
        long remaining = limit - bytesRead;
        return (int) Math.max(0, Math.min(super.available(), remaining));
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            cancelWatchdog();
            super.close();
        }
    }

    private void safeClose() {
        try {
            close();
        } catch (IOException _) {
        }
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void mark(int readlimit) {

    }

    @Override
    public void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }

    private boolean isLimitReached() {
        return bytesRead >= limit;
    }

    private void cancelWatchdog() {
        if (watchdogTask != null && !watchdogTask.isDone()) {
            watchdogTask.cancel(true);
        }
    }

    private void ensureOpen() throws IOException {
        if (closed.get()) {
            throw new IOException("Stream already closed");
        }
    }
}
