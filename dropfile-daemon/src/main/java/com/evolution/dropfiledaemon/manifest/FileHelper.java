package com.evolution.dropfiledaemon.manifest;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.input.BoundedInputStream;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.function.Consumer;

@Component
public class FileHelper {

    private static final int BUFFER_SIZE = 64 * 1024;

    private static final String SHA256 = "SHA-256";

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

    public void write(FileChannel fileChannel,
                      InputStream inputStream,
                      MessageDigest digest,
                      long position,
                      int size) throws IOException {
        byte[] buffer = new byte[size]; // TODO use small buffer
        while (true) {
            int read = inputStream.read(buffer);
            if (read == -1) {
                break;
            }
            fileChannel.write(ByteBuffer.wrap(buffer, 0, read), position);
            digest.update(buffer, 0, read);
        }
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public byte[] readBytes(FileChannel fileChannel, long skip, int take) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(take);
        fileChannel.position(skip);
        fileChannel.read(buffer);
        return buffer.array();
    }

    @Data
    @RequiredArgsConstructor
    public static class ChunkContainer {
        private final byte[] data;
        private final long from;
        private final long to;
    }
}
