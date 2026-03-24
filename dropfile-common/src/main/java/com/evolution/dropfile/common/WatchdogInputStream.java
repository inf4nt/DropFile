package com.evolution.dropfile.common;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class WatchdogInputStream extends FilterInputStream {

    private static final ExecutorService WATCHDOG_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> WATCHDOG_EXECUTOR.shutdownNow()));
    }

    private final long limit;

    final Future<?> watchdogTask;

    private long bytesRead;

    volatile private boolean closed;

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
    public int read() throws IOException {
        if (closed) {
            throw new IOException("Stream already closed");
        }

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
        if (closed) {
            throw new IOException("Stream already closed");
        }

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
    public void close() {
        if (!closed) {
            finalizeStream();
            CommonUtils.executeSafety(() -> super.close());
            closed = true;
        }
    }

    private void finalizeStream() {
        if (watchdogTask != null && !watchdogTask.isDone()) {
            watchdogTask.cancel(true);
        }
    }
}
