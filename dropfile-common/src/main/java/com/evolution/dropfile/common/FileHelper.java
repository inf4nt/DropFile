package com.evolution.dropfile.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class FileHelper {

    private static final String SHA256 = "SHA-256";

    private final int bufferSize;

    public FileHelper(int bufferSize) {
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("bufferSize must be greater than zero");
        }
        this.bufferSize = bufferSize;
    }

    public InputStream readStream(File file, long skip, int take) throws IOException {
        FileChannel fileChannel = null;
        try {
            fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
            fileChannel.position(skip);
            InputStream inputStream = Channels.newInputStream(fileChannel);
            return new WatchdogInputStream(inputStream, take);
        } catch (IOException e) {
            if (fileChannel != null) {
                fileChannel.close();
            }
            throw e;
        }
    }

    public void write(Path path, byte[] data) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE)) {
            write(channel, data, 0);
        }
    }

    public void write(FileChannel fileChannel,
                      byte[] bytes,
                      long position) throws IOException {
        ByteBuffer wrap = ByteBuffer.wrap(bytes);
        long offset = position;

        while (wrap.hasRemaining()) {
            CommonUtils.isInterrupted();

            int originalLimit = wrap.limit();

            int chunk = Math.min(wrap.remaining(), bufferSize);
            wrap.limit(wrap.position() + chunk);

            int written = fileChannel.write(wrap, offset);
            if (written <= 0) {
                throw new IOException("FileChannel.write wrote zero or negative numbers of bytes");
            }
            offset += written;
            wrap.limit(originalLimit);
        }
    }

    @Deprecated
    // TODO use CommonUtils
    public String sha256(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(SHA256);
        return HexFormat.of().formatHex(digest.digest(data));
    }

    public String sha256(Path path) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance(SHA256);
        byte[] buffer = new byte[bufferSize];
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ);
             InputStream inputStream = Channels.newInputStream(channel)) {
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                CommonUtils.isInterrupted();

                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
