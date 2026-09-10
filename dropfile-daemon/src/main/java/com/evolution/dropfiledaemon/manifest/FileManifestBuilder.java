package com.evolution.dropfiledaemon.manifest;

import com.evolution.dropfile.common.io.FileHelper;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Deprecated
@Slf4j
@Component
public class FileManifestBuilder {

    private final AtomicBoolean closed = new AtomicBoolean();

    private final FileHelper fileHelper;

    private final int chunkMaxSize;

    @Autowired
    public FileManifestBuilder(DaemonApplicationProperties daemonApplicationProperties, FileHelper fileHelper) {
        this(daemonApplicationProperties.daemonManifestChunkMaxSize, fileHelper);
    }

    FileManifestBuilder(int chunkMaxSize, FileHelper fileHelper) {
        this.chunkMaxSize = chunkMaxSize;
        this.fileHelper = fileHelper;
    }

    public void validate(FileManifest fileManifest) {
        List<ChunkManifest> chunks = fileManifest.chunkManifests();

        if (chunks == null || chunks.isEmpty()) {
            throw new IllegalArgumentException("File manifest has no chunk manifests");
        }

        long totalSizeByChunks = 0;
        for (ChunkManifest chunk : chunks) {
            if (chunk.size() <= 0) {
                throw new IllegalArgumentException("Found zero or negative chunk size");
            }
            if (chunk.position() < 0) {
                throw new IllegalArgumentException("Chunk position must be greater or equal to zero");
            }
            if (chunk.size() > chunkMaxSize) {
                throw new IllegalArgumentException("File manifest has oversized chunk manifests");
            }
            totalSizeByChunks += chunk.size();
        }

        if (totalSizeByChunks != fileManifest.size()) {
            throw new IllegalArgumentException("File manifest size does not match total chunks size");
        }

        List<ChunkManifest> sortedChunks = chunks.stream()
                .sorted(Comparator.comparingLong(ChunkManifest::position))
                .toList();

        long expectedPosition = 0;
        for (ChunkManifest chunk : sortedChunks) {
            if (chunk.position() != expectedPosition) {
                throw new IllegalArgumentException(
                        "Gaps or overlaps detected. Expected position: " + expectedPosition + ", but got: " + chunk.position()
                );
            }
            expectedPosition += chunk.size();
        }
    }

    public FileManifest build(Path source,
                              String fileManifestName,
                              int chunkSize) throws IOException, NoSuchAlgorithmException {
        if (!StringUtils.hasText(fileManifestName)) {
            throw new IllegalArgumentException("File manifest name is empty");
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be greater than zero");
        }
        if (!Files.exists(source)) {
            throw new FileNotFoundException("No file found: " + source);
        }
        if (!Files.isRegularFile(source)) {
            throw new IllegalArgumentException("File is not a regular file: " + source.toAbsolutePath());
        }

        long fileSize = Files.size(source);

        List<ChunkManifest> chunkManifests = new ArrayList<>();

        long position = 0;
        while (position < fileSize) {
            int bytesInChunk = (int) Math.min(chunkSize, fileSize - position);
            chunkManifests.add(new ChunkManifest(bytesInChunk, position));
            position += bytesInChunk;
        }

        checkIfClosed();
        String hash = fileHelper.sha256(source);

        return new FileManifest(
                fileManifestName,
                hash,
                fileSize,
                chunkManifests
        );
    }

    public int getChunkSize(int requestChunkSize) {
        return Math.min(requestChunkSize, chunkMaxSize);
    }

    @EventListener(ContextClosedEvent.class)
    public void contextClosedEventListener() {
        boolean set = closed.compareAndSet(false, true);
        if (!set) {
            return;
        }

        log.info("Closing {} by {}", FileManifestBuilder.class, ContextClosedEvent.class);
    }

    private void checkIfClosed() {
        if (closed.get()) {
            throw new IllegalStateException("Already closed " + FileManifestBuilder.class);
        }
    }
}
