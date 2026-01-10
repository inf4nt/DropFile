package com.evolution.dropfiledaemon.manifest;

import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Component
public class FileHelper {

    private static final String SHA256 = "SHA-256";

    @SneakyThrows
    public List<byte[]> read(File file, int chunkSizeFactor) {
        long fileLength = file.length();
        List<byte[]> chunks = new ArrayList<>();
        long processed = 0;
        try (FileInputStream targetStream = new FileInputStream(file)) {
            while (processed < fileLength) {
                int chunkToProcess = chunkSizeFactor;
                long leftOver = fileLength - processed;
                if (leftOver < chunkToProcess) {
                    chunkToProcess = Math.toIntExact(leftOver);
                }

                byte[] chunk = readBytes(targetStream, processed, chunkToProcess);
                chunks.add(chunk);
                processed = processed + chunk.length;
            }
        }

        return chunks;
    }

    @SneakyThrows
    public void read(File file, int chunkSizeFactor, Consumer<ChunkContainer> consumer) {
        long fileLength = file.length();
        long processed = 0;
        try (FileInputStream targetStream = new FileInputStream(file)) {
            while (processed < fileLength) {
                int chunkToProcess = chunkSizeFactor;
                long leftOver = fileLength - processed;
                if (leftOver < chunkToProcess) {
                    chunkToProcess = Math.toIntExact(leftOver);
                }

                byte[] chunk = readBytes(targetStream, processed, chunkToProcess);
                consumer.accept(new ChunkContainer(chunk, processed, processed + chunkToProcess));
                processed = processed + chunk.length;
            }
        }
    }

    @SneakyThrows
    public byte[] read(File file, long skip, int take) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            return readBytes(fileInputStream, skip, take);
        }
    }

    @SneakyThrows
    public byte[] readBytes(FileInputStream inputStream, long skip, int take) {
        ByteBuffer buffer = ByteBuffer.allocate(take);
        FileChannel fileChannel = inputStream.getChannel();
        fileChannel.read(buffer, skip);
        return buffer.array();
    }

    @SneakyThrows
    public String sha256(byte[] data) {
        MessageDigest digest = MessageDigest.getInstance(SHA256);
        byte[] hashBytes = digest.digest(data);
        return bytesToHex(hashBytes);
    }

    @SneakyThrows
    public String sha256(File file) {
        MessageDigest digest = MessageDigest.getInstance(SHA256);

        byte[] buffer = new byte[64 * 1024];
        int bytesRead;

        try (FileInputStream fis = new FileInputStream(file)) {
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        byte[] hash = digest.digest();
        return bytesToHex(hash);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public record ChunkContainer(byte[] data, long from, long to) {

    }
}
