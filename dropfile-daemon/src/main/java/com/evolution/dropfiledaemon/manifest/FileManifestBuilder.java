package com.evolution.dropfiledaemon.manifest;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
public class FileManifestBuilder {

    private static final String SHA256 = "SHA-256";

    private final int chunkMaxSize;

    private final int bufferSize;

    @Autowired
    public FileManifestBuilder(DaemonApplicationProperties daemonApplicationProperties) {
        this(daemonApplicationProperties.manifestChunkMaxSize, daemonApplicationProperties.manifestBuildBufferSize);
    }

    FileManifestBuilder(int chunkMaxSize, int bufferSize) {
        this.chunkMaxSize = chunkMaxSize;
        this.bufferSize = bufferSize;
    }

    public void validate(FileManifest fileManifest) {
        if (fileManifest.chunkManifests() == null || fileManifest.chunkManifests().isEmpty()) {
            throw new IllegalStateException("File manifest has no chunk manifests");
        }

        boolean zeroSizeChunk = fileManifest.chunkManifests().stream()
                .anyMatch(it -> it.size() <= 0);
        if (zeroSizeChunk) {
            throw new RuntimeException("Found zero chunks size. Chunks size must be greater than zero");
        }

        long zeroStartPosition = fileManifest.chunkManifests().stream()
                .filter(it -> it.startPosition() == 0)
                .count();
        if (zeroStartPosition != 1) {
            throw new RuntimeException("Chunks must include one chunk with zero start position");
        }

        boolean negativeStart = fileManifest.chunkManifests().stream()
                .anyMatch(it -> it.startPosition() < 0 || it.endPosition() <= 0);
        if (negativeStart) {
            throw new RuntimeException("Chunks start must be greater or equal zero");
        }

        boolean negativeOrZeroEnd = fileManifest.chunkManifests().stream()
                .anyMatch(it -> it.endPosition() <= 0);
        if (negativeOrZeroEnd) {
            throw new RuntimeException("Chunks end must be greater than zero");
        }

        boolean oversized = fileManifest.chunkManifests().stream()
                .anyMatch(it -> it.size() > chunkMaxSize);
        if (oversized) {
            throw new RuntimeException("File manifest has oversized chunk manifests");
        }

        boolean theEndIsNotBeforeTheStart = fileManifest.chunkManifests().stream()
                .anyMatch(it -> it.startPosition() >= it.endPosition());
        if (theEndIsNotBeforeTheStart) {
            throw new RuntimeException("The chunk end position must be after the start position");
        }

        long totalSizeByChunkSize = fileManifest.chunkManifests()
                .stream().map(it -> it.size())
                .collect(Collectors.summarizingLong(x -> x))
                .getSum();
        if (totalSizeByChunkSize != fileManifest.size()) {
            throw new RuntimeException("File manifest size does not match chunks size");
        }

        long totalSizeByStartAndEnd = fileManifest.chunkManifests().stream()
                .map(it -> {
                    long start = it.startPosition();
                    long end = it.endPosition();
                    if (start == 0) {
                        return end;
                    }
                    return end - start;
                })
                .collect(Collectors.summarizingLong(value -> value)).getSum();
        if (totalSizeByStartAndEnd != fileManifest.size()) {
            throw new RuntimeException("File manifest size does not match chunks start and end size");
        }
    }

    @SneakyThrows
    public FileManifest build(File file, int chunkSize) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be greater than zero");
        }

        if (!Files.exists(file.toPath())) {
            throw new FileNotFoundException("No file found: " + file.getAbsolutePath());
        }

        if (Files.isDirectory(file.toPath())) {
            throw new UnsupportedOperationException("Directories are unsupported: " + file.getAbsolutePath());
        }

        List<ChunkManifest> chunkManifests = Collections.synchronizedList(new ArrayList<>());
        int chunkSizeFactor = Math.toIntExact(Math.min(chunkSize, file.length()));

        MessageDigest manifestDigest = MessageDigest.getInstance(SHA256);
        MessageDigest chunkDigest = MessageDigest.getInstance(SHA256);

        long fileLength = file.length();
        long processed = 0;

        ByteBuffer byteBuffer = ByteBuffer.allocate((int) Math.min(file.length(), bufferSize));

        try (FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
            while (processed < fileLength) {
                int chunkToProcess = chunkSizeFactor;
                long leftOver = fileLength - processed;
                if (leftOver < chunkToProcess) {
                    chunkToProcess = Math.toIntExact(leftOver);
                }

                int totalRead = read(fileChannel, processed, chunkToProcess, byteBuffer, buffer -> {
                    manifestDigest.update(buffer.duplicate());
                    chunkDigest.update(buffer);
                });
                ChunkManifest chunkManifest = new ChunkManifest(
                        HexFormat.of().formatHex(chunkDigest.digest()),
                        totalRead,
                        processed,
                        processed + chunkToProcess
                );
                chunkManifests.add(chunkManifest);

                processed = processed + totalRead;
            }
        }

        long totalSize = chunkManifests.stream().map(it -> it.size())
                .mapToLong(Long::valueOf)
                .sum();

        return new FileManifest(
                file.getName(),
                HexFormat.of().formatHex(manifestDigest.digest()),
                totalSize,
                chunkManifests
        );
    }

    public int getChunkSize(int requestChunkSize) {
        return Math.min(requestChunkSize, chunkMaxSize);
    }

    int read(FileChannel fileChannel,
             long skip,
             int take,
             ByteBuffer byteBuffer,
             Consumer<ByteBuffer> consumer) throws IOException {
        fileChannel.position(skip);
        int totalRead = 0;
        while (totalRead < take) {
            CommonUtils.isInterrupted();

            int leftOver = take - totalRead;
            int toRead = Math.min(byteBuffer.capacity(), leftOver);
            byteBuffer.limit(toRead);

            int read = fileChannel.read(byteBuffer);
            if (read == -1) {
                break;
            }

            byteBuffer.flip();

            consumer.accept(byteBuffer.asReadOnlyBuffer());

            totalRead += read;

            byteBuffer.clear();
        }
        return totalRead;
    }
}
