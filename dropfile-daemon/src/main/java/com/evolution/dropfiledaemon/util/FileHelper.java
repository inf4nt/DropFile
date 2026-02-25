package com.evolution.dropfiledaemon.util;

import com.evolution.dropfile.common.CommonUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.input.BoundedInputStream;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.function.Consumer;

@Component
public class FileHelper {

    private static final int BUFFER_SIZE = 64 * 1_024;

    private static final String SHA256 = "SHA-256";

    // TODO finish me
    public void sha256(File file, int chunkSizeFactor, Consumer<Sha256Container> consumer) throws IOException {
        long fileLength = file.length();
        long processed = 0;
        try (FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
            while (processed < fileLength) {
                int chunkToProcess = chunkSizeFactor;
                long leftOver = fileLength - processed;
                if (leftOver < chunkToProcess) {
                    chunkToProcess = Math.toIntExact(leftOver);
                }

                byte[] sha256 = getSha256(fileChannel, processed, chunkToProcess);
                consumer.accept(new Sha256Container(sha256, processed, processed + chunkToProcess, chunkToProcess));
                int length = chunkToProcess;
                processed = processed + length;
            }
        }
    }

    public void read(File file, int chunkSizeFactor, Consumer<ChunkContainer> consumer) throws IOException {
        long fileLength = file.length();
        long processed = 0;
        try (FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
            while (processed < fileLength) {
                int chunkToProcess = chunkSizeFactor;
                long leftOver = fileLength - processed;
                if (leftOver < chunkToProcess) {
                    chunkToProcess = Math.toIntExact(leftOver);
                }

                byte[] chunk = readBytes(fileChannel, processed, chunkToProcess);
                consumer.accept(new ChunkContainer(chunk, processed, processed + chunkToProcess));
                processed = processed + chunk.length;
            }
        }
    }

    public InputStream readStream(File file, long skip, int take) throws IOException {
        FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
        try {
            fileChannel.position(skip);
            InputStream inputStream = Channels.newInputStream(fileChannel);
            return BoundedInputStream.builder()
                    .setInputStream(inputStream)
                    .setMaxCount(take)
                    .get();
        } catch (IOException e) {
            fileChannel.close();
            throw e;
        }
    }

    @SneakyThrows
    public String sha256(byte[] data) {
        MessageDigest digest = MessageDigest.getInstance(SHA256);
        byte[] hashBytes = digest.digest(data);
        return bytesToHex(hashBytes);
    }

    @SneakyThrows
    public String sha256(File file) {
        return sha256(file, BUFFER_SIZE);
    }

    @SneakyThrows
    String sha256(File file, int bufferSize) {
        MessageDigest digest = MessageDigest.getInstance(SHA256);
        byte[] buffer = new byte[bufferSize];
        try (InputStream inputStream = new FileInputStream(file)) {
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                CommonUtils.isInterrupted();
                digest.update(buffer, 0, read);
            }
        }
        return bytesToHex(digest.digest());
    }

    public void write(FileChannel fileChannel,
                      InputStream inputStream,
                      long position) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long offset = position;
        while (true) {
            int read = inputStream.read(buffer);
            if (read == -1) {
                break;
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, read);

            while (byteBuffer.hasRemaining()) {
                int written = fileChannel.write(byteBuffer, offset);
                offset += written;
            }
        }
    }

    public void write(FileChannel fileChannel,
                      byte[] bytes,
                      long position) throws IOException {
        ByteBuffer wrap = ByteBuffer.wrap(bytes);
        long offset = position;
        while (wrap.hasRemaining()) {
            int written = fileChannel.write(wrap, offset);
            if (written <= 0) {
                throw new IOException("FileChannel.write wrote zero or negative numbers of bytes");
            }
            offset += written;
        }
    }

    public String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public byte[] getSha256(FileChannel fileChannel, long skip, int take) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        return null;
    }

    public byte[] readBytes(FileChannel fileChannel, long skip, int take) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(take);
        fileChannel.position(skip);
        fileChannel.read(buffer);
        return buffer.array();
    }

    public String percent(long total, long downloaded) {
        if (total == 0) {
            return "0%";
        }
        if (downloaded == 0) {
            return "0%";
        }
        if (total == downloaded) {
            return "100%";
        }

        double value = (double) (downloaded * 100) / total;
        return String.format(Locale.US, "%.2f%%", value);
    }

    public String toDisplaySize(long size) {
        if (size < 0) {
            throw new IllegalArgumentException("Size cannot be negative");
        }
        if (size == 0) {
            return "0B";
        }
        if (size < 1024) {
            return String.format("%sB", size);
        }
        if (size < 1024 * 1024) {
            double kb = size / 1024D;
            return String.format(Locale.US, "%.2fKB", kb);
        }
        if (size < 1024 * 1024 * 1024) {
            double mb = size / (1024 * 1024D);
            return String.format(Locale.US, "%.2fMB", mb);
        }
        double gb = size / (1024 * 1024 * 1024D);
        return String.format(Locale.US, "%.2fGB", gb);
    }

    @Data
    @RequiredArgsConstructor
    public static class ChunkContainer {
        private final byte[] data;
        private final long from;
        private final long to;
    }

    @Data
    @RequiredArgsConstructor
    public static class Sha256Container {
        private final byte[] sha256;
        private final long from;
        private final long to;
        private final int length;
    }
}
