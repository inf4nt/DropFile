package com.evolution.dropfiledaemon;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public class DiskSpaceGuardedOutputStream extends OutputStream {

    private final OutputStream delegate;
    private final DiskSpaceGuard diskSpaceGuard;
    private final long checkIntervalBytes;

    private long bytesUntilCheck;

    public DiskSpaceGuardedOutputStream(OutputStream delegate, DiskSpaceGuard diskSpaceGuard) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.diskSpaceGuard = Objects.requireNonNull(diskSpaceGuard, "diskSpaceGuard must not be null");
        this.checkIntervalBytes = diskSpaceGuard.getCheckIntervalBytes();
        this.bytesUntilCheck = 0;
    }

    @Override
    public void write(int b) throws IOException {
        diskSpaceGuard.assertHealthy();

        if (bytesUntilCheck == 0) {
            diskSpaceGuard.check(checkIntervalBytes);
            bytesUntilCheck = checkIntervalBytes;
        }

        try {
            delegate.write(b);
        } catch (IOException e) {
            throw diskSpaceGuard.handleWriteFailure(e);
        }

        bytesUntilCheck--;
    }

    @Override
    public void write(byte[] bytes, int off, int len) throws IOException {
        Objects.requireNonNull(bytes, "bytes must not be null");
        if (off < 0 || len < 0 || len > bytes.length - off) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return;
        }

        int currentOffset = off;
        int remaining = len;

        while (remaining > 0) {
            diskSpaceGuard.assertHealthy();

            if (bytesUntilCheck == 0) {
                diskSpaceGuard.check(checkIntervalBytes);
                bytesUntilCheck = checkIntervalBytes;
            }

            int chunkSize = (int) Math.min(remaining, bytesUntilCheck);

            try {
                delegate.write(bytes, currentOffset, chunkSize);
            } catch (IOException e) {
                throw diskSpaceGuard.handleWriteFailure(e);
            }

            currentOffset += chunkSize;
            remaining -= chunkSize;
            bytesUntilCheck -= chunkSize;
        }
    }

    @Override
    public void flush() throws IOException {
        diskSpaceGuard.assertHealthy();
        try {
            delegate.flush();
        } catch (IOException e) {
            throw diskSpaceGuard.handleWriteFailure(e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            delegate.close();
        } catch (IOException e) {
            throw diskSpaceGuard.handleWriteFailure(e);
        }
    }
}