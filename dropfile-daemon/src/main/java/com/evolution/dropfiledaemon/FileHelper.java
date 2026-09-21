package com.evolution.dropfiledaemon;

import com.evolution.dropfile.common.io.CloseShieldInputStream;
import com.evolution.dropfile.common.io.CloseShieldOutputStream;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

public class FileHelper {

    private final DiskSpaceGuard diskSpaceGuard;

    public FileHelper(DiskSpaceGuard diskSpaceGuard) {
        this.diskSpaceGuard = diskSpaceGuard;
    }

    public void write(FileChannel fileChannel,
                      InputStream inputStream,
                      long position,
                      long size) throws IOException {

        try (ReadableByteChannel readableByteChannel = Channels.newChannel(CloseShieldInputStream.stream(inputStream))) {

            long offset = position;
            long remaining = size;
            long bytesUntilCheck = 0;

            while (remaining > 0) {
                diskSpaceGuard.assertHealthy();

                if (bytesUntilCheck == 0) {
                    diskSpaceGuard.check(diskSpaceGuard.getCheckIntervalBytes());
                    bytesUntilCheck = diskSpaceGuard.getCheckIntervalBytes();
                }

                long chunkSize = Math.min(remaining, bytesUntilCheck);
                long transferred;

                try {
                    transferred = fileChannel.transferFrom(readableByteChannel, offset, chunkSize);
                } catch (IOException e) {
                    throw diskSpaceGuard.handleWriteFailure(e);
                }

                if (transferred <= 0) {
                    throw new EOFException(String.format(
                            "Premature EOF: Failed to transfer entire file content. " +
                                    "Expected %d bytes, but was missing %d bytes",
                            size,
                            remaining
                    ));
                }

                offset += transferred;
                remaining -= transferred;
                bytesUntilCheck -= transferred;
            }
        }
    }

    public void outputStreamConsumer(Path path, Consumer<java.io.OutputStream> outputStreamConsumer) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE)) {
            try (DiskSpaceGuardedOutputStream out = new DiskSpaceGuardedOutputStream(
                    Channels.newOutputStream(channel),
                    diskSpaceGuard
            )) {
                outputStreamConsumer.accept(CloseShieldOutputStream.stream(out));
                out.flush();
            }
        }
    }
}