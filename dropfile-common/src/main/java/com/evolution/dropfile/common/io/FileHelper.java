package com.evolution.dropfile.common.io;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.function.OutputStreamConsumer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class FileHelper {

    private static final String SHA256 = "SHA-256";

    public void transferTo(Path path, OutputStream outputStreamArgument) throws IOException {
        InterruptibleOutputStream outputStream = InterruptibleOutputStream.stream(
                CloseShieldOutputStream.stream(outputStreamArgument)
        );

        try (FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ);
             WritableByteChannel writableByteChannel = Channels.newChannel(outputStream)) {

            long position = 0;
            long size = fileChannel.size();

            while (position < size) {
                long transferred = fileChannel.transferTo(position, size - position, writableByteChannel);

                if (transferred <= 0) {
                    throw new IOException(String.format(
                            "Failed to transfer file content: '%s'. Expected to read %d bytes, but got 0. " +
                                    "The file might have been concurrently truncated or modified during the transfer.",
                            path, size - position
                    ));
                }

                position += transferred;
            }
        }
    }

    public void write(Path path, InputStream inputStream) throws IOException {
        try (FileChannel channel = FileChannel.open(path,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            write(channel, inputStream, 0, Long.MAX_VALUE);
        }
    }

    public void write(FileChannel fileChannel,
                      InputStream inputStream,
                      long position,
                      long size) throws IOException {
        try (ReadableByteChannel readableByteChannel = Channels.newChannel(CloseShieldInputStream.stream(inputStream))) {
            long offset = position;
            long remaining = size;

            while (remaining > 0) {
                long transferred = fileChannel.transferFrom(readableByteChannel, offset, remaining);

                if (transferred <= 0) {
                    throw new IOException(String.format(
                            "Premature EOF: Failed to transfer entire file content. Expected %d bytes, but was missing %d bytes",
                            size, remaining
                    ));
                }

                offset += transferred;
                remaining -= transferred;
            }
        }
    }

    public InputStream readStream(Path path, long skip, int take) throws IOException {
        FileChannel fileChannel = null;
        InputStream channelInputStream = null;
        try {
            fileChannel = FileChannel.open(path, StandardOpenOption.READ);
            fileChannel.position(skip);
            channelInputStream = Channels.newInputStream(fileChannel);
            return new WatchdogInputStream(channelInputStream, take);
        } catch (Throwable throwable) {
            if (channelInputStream != null) {
                try {
                    channelInputStream.close();
                } catch (Throwable channelInputStreamThrowable) {
                    throwable.addSuppressed(channelInputStreamThrowable);
                }
            } else if (fileChannel != null) {
                try {
                    fileChannel.close();
                } catch (Throwable fileChannelThrowable) {
                    throwable.addSuppressed(fileChannelThrowable);
                }
            }

            if (throwable instanceof IOException ioException) {
                throw ioException;
            }
            throw CommonUtils.toRuntimeException(throwable);
        }
    }

    public void outputStreamConsumer(Path path, OutputStreamConsumer outputStreamConsumer) throws IOException {
        try (FileChannel channel = FileChannel.open(
                path,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);
             OutputStream out = Channels.newOutputStream(channel)) {
            outputStreamConsumer.accept(CloseShieldOutputStream.stream(out));
            out.flush();
        }
    }

    public String sha256(Path path) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance(SHA256);

        OutputStream digestOutputStream = InterruptibleOutputStream.stream(
                new OutputStream() {
                    @Override
                    public void write(int b) {
                        digest.update((byte) b);
                    }

                    @Override
                    public void write(byte[] b, int off, int len) {
                        digest.update(b, off, len);
                    }
                }
        );

        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ);
             InputStream inputStream = Channels.newInputStream(channel)) {

            long expectedSize = channel.size();
            long transferred = inputStream.transferTo(digestOutputStream);

            if (transferred != expectedSize) {
                throw new IOException(String.format(
                        "Concurrent modification detected: Expected to hash %d bytes, but read %d bytes from %s",
                        expectedSize, transferred, path
                ));
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
