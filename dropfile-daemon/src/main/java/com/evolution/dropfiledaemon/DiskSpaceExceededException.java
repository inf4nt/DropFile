package com.evolution.dropfiledaemon;


import java.io.IOException;
import java.nio.file.Path;

public class DiskSpaceExceededException extends IOException {

    private final Path path;
    private final long usableSpaceBytes;
    private final long requiredBytes;
    private final long minimumFreeSpaceBytes;

    public DiskSpaceExceededException(Path path,
                                      long usableSpaceBytes,
                                      long requiredBytes,
                                      long minimumFreeSpaceBytes) {
        super("""
                Insufficient disk space on path '%s'. \
                Available: %d bytes, required for write: %d bytes, \
                minimum required free space: %d bytes
                """.formatted(
                path.toAbsolutePath(),
                usableSpaceBytes,
                requiredBytes,
                minimumFreeSpaceBytes
        ));

        this.path = path;
        this.usableSpaceBytes = usableSpaceBytes;
        this.requiredBytes = requiredBytes;
        this.minimumFreeSpaceBytes = minimumFreeSpaceBytes;
    }

    public Path getPath() {
        return path;
    }

    public long getUsableSpaceBytes() {
        return usableSpaceBytes;
    }

    public long getRequiredBytes() {
        return requiredBytes;
    }

    public long getMinimumFreeSpaceBytes() {
        return minimumFreeSpaceBytes;
    }
}