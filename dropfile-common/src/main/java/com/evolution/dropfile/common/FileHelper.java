package com.evolution.dropfile.common;

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

    public void transferTo(Path path, OutputStream outputStream) throws IOException {
        CloseShieldOutputStream closeShieldOutputStream = new CloseShieldOutputStream(outputStream);

        try (FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ);
             WritableByteChannel writableByteChannel = Channels.newChannel(closeShieldOutputStream)) {
            fileChannel.transferTo(0, Long.MAX_VALUE, writableByteChannel);
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
        CloseShieldInputStream closeShieldInputStream = new CloseShieldInputStream(inputStream);
        try (ReadableByteChannel readableByteChannel = Channels.newChannel(closeShieldInputStream)) {
            long offset = position;
            long remaining = size;

            while (remaining > 0) {
                long transferred = fileChannel.transferFrom(readableByteChannel, offset, remaining);

                if (transferred <= 0) {
                    break;
                }

                offset += transferred;
                remaining -= transferred;
            }
        }
    }

    public InputStream readStream(Path path, long skip, int take) throws IOException {
        FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ);
        boolean success = false;
        try {
            fileChannel.position(skip);
            InputStream inputStream = Channels.newInputStream(fileChannel);
            InputStream watchdog = new WatchdogInputStream(inputStream, take);
            success = true;
            return watchdog;
        } finally {
            if (!success) {
                fileChannel.close();
            }
        }
    }

    public String sha256(Path path) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance(SHA256);

        OutputStream digestOutputStream = new OutputStream() {
            @Override
            public void write(int b) {
                digest.update((byte) b);
            }

            @Override
            public void write(byte[] b, int off, int len) {
                digest.update(b, off, len);
            }
        };

        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ);
             InputStream inputStream = Channels.newInputStream(channel)) {
            inputStream.transferTo(digestOutputStream);
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
