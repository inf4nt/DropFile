package com.evolution.dropfiledaemon;

import com.evolution.dropfile.store.framework.file.DirectoryProvider;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class DiskSpaceGuard {

    public static final long DEFAULT_MIN_REQUIRED_FREE_SPACE_BYTES = 1024L * 1024 * 1024;
    public static final long DEFAULT_CHECK_INTERVAL_BYTES = 16L * 1024 * 1024;

    private final DirectoryProvider directoryProvider;
    private final long minRequiredFreeSpaceBytes;
    private final long checkIntervalBytes;

    private final AtomicReference<DiskSpaceExceededException> failure = new AtomicReference<>();

    public DiskSpaceGuard(DirectoryProvider directoryProvider) {
        this(directoryProvider, DEFAULT_MIN_REQUIRED_FREE_SPACE_BYTES, DEFAULT_CHECK_INTERVAL_BYTES);
    }

    public DiskSpaceGuard(DirectoryProvider directoryProvider,
                          long minRequiredFreeSpaceBytes,
                          long checkIntervalBytes) {
        this.directoryProvider = Objects.requireNonNull(directoryProvider, "directoryProvider must not be null");

        if (minRequiredFreeSpaceBytes < 0) {
            throw new IllegalArgumentException("minRequiredFreeSpaceBytes cannot be negative");
        }
        if (checkIntervalBytes <= 0) {
            throw new IllegalArgumentException("checkIntervalBytes must be greater than zero");
        }

        this.minRequiredFreeSpaceBytes = minRequiredFreeSpaceBytes;
        this.checkIntervalBytes = checkIntervalBytes;
    }

    public Path getStoragePath() {
        return directoryProvider.getDirectoryPath();
    }

    public long getCheckIntervalBytes() {
        return checkIntervalBytes;
    }

    public void assertHealthy() throws DiskSpaceExceededException {
        DiskSpaceExceededException currentFailure = failure.get();
        if (currentFailure != null) {
            throw currentFailure;
        }
    }

    public void check(long bytesToWrite) throws IOException {
        if (bytesToWrite < 0) {
            throw new IllegalArgumentException("bytesToWrite cannot be negative");
        }

        assertHealthy();

        long usableSpaceBytes = getUsableSpace();

        if (usableSpaceBytes < bytesToWrite || usableSpaceBytes - bytesToWrite < minRequiredFreeSpaceBytes) {
            DiskSpaceExceededException exception = new DiskSpaceExceededException(
                    getStoragePath(),
                    usableSpaceBytes,
                    bytesToWrite,
                    minRequiredFreeSpaceBytes
            );
            failure.compareAndSet(null, exception);
            throw failure.get();
        }
    }

    public IOException handleWriteFailure(IOException cause) {
        DiskSpaceExceededException currentFailure = failure.get();
        if (currentFailure != null) {
            return currentFailure;
        }

        long usableSpaceBytes = getUsableSpace();

        if (usableSpaceBytes < minRequiredFreeSpaceBytes) {
            DiskSpaceExceededException exception = new DiskSpaceExceededException(
                    getStoragePath(),
                    usableSpaceBytes,
                    0,
                    minRequiredFreeSpaceBytes
            );
            failure.compareAndSet(null, exception);
            return failure.get();
        }
        return cause;
    }

    public void reset() {
        failure.set(null);
    }

    public boolean isTripped() {
        return failure.get() != null;
    }

    private long getUsableSpace() {
        return getStoragePath().toFile().getUsableSpace();
    }
}