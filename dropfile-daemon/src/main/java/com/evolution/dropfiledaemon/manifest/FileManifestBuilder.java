package com.evolution.dropfiledaemon.manifest;

import com.evolution.dropfiledaemon.util.FileHelper;
import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.*;

@Component
public class FileManifestBuilder {

    private static final String SHA256 = "SHA-256";

    @Getter
    private final Integer chunkSize;

    private final Integer bufferSize;

    private final FileHelper fileHelper;

    @Autowired
    public FileManifestBuilder(@Value("${file.manifest.builder.chunk.size}") Integer chunkSize,
                               @Value("${file.manifest.builder.buffer.size}") Integer bufferSize,
                               FileHelper fileHelper) {
        this.chunkSize = Objects.requireNonNull(chunkSize, "chunkSize is null");
        this.bufferSize = Objects.requireNonNull(bufferSize, "bufferSize is null");
        this.fileHelper = fileHelper;
    }

    @SneakyThrows
    public FileManifest build(File file) {
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

                int totalRead = fileHelper.read(fileChannel, processed, chunkToProcess, byteBuffer, buffer -> {
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
}
