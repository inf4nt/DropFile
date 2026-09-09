package com.evolution.dropfiledaemon.manifest;

import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class FileManifestBuilder {

    private final AtomicBoolean closed = new AtomicBoolean();

    private static final String SHA256 = "SHA-256";

    private static final HexFormat HEX_FORMAT = HexFormat.of();

    private final int chunkMaxSize;

    @Autowired
    public FileManifestBuilder(DaemonApplicationProperties daemonApplicationProperties) {
        this(daemonApplicationProperties.daemonManifestChunkMaxSize);
    }

    FileManifestBuilder(int chunkMaxSize) {
        this.chunkMaxSize = chunkMaxSize;
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

    public FileManifest build(Path source, String fileManifestName, int chunkSize) throws IOException, NoSuchAlgorithmException {
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

        int expectedChunks = (int) Math.ceil((double) fileSize / chunkSize);
        List<ChunkManifest> chunkManifests = new ArrayList<>(Math.max(expectedChunks, 1));

        long totalSizeAccumulated = 0;

        MessageDigest manifestDigest = MessageDigest.getInstance(SHA256);
        MessageDigest chunkDigest = MessageDigest.getInstance(SHA256);

        int bufferSize = Math.min(64 * 1024, chunkSize);
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);

        try (FileChannel fileChannel = FileChannel.open(source, StandardOpenOption.READ)) {
            long position = 0;

            while (position < fileSize) {
                checkIfClosed();

                int bytesInChunk = (int) Math.min(chunkSize, fileSize - position);
                int bytesReadInChunk = 0;

                while (bytesReadInChunk < bytesInChunk) {
                    int toRead = Math.min(buffer.capacity(), bytesInChunk - bytesReadInChunk);
                    buffer.limit(toRead);

                    int read = fileChannel.read(buffer);
                    if (read == -1) {
                        throw new IOException("Unexpected EOF or channel closed during chunk transfer");
                    }

                    byte[] array = buffer.array();
                    manifestDigest.update(array, 0, read);
                    chunkDigest.update(array, 0, read);

                    bytesReadInChunk += read;
                    buffer.clear();
                }

                byte[] chunkHash = chunkDigest.digest();
                ChunkManifest chunkManifest = new ChunkManifest(HEX_FORMAT.formatHex(chunkHash), bytesInChunk, position);
                chunkManifests.add(chunkManifest);

                position += bytesInChunk;
                totalSizeAccumulated += bytesInChunk;
            }
        }

        if (totalSizeAccumulated != fileSize) {
            throw new IOException("Calculated size does not match file size: " + fileSize + " total size: " + totalSizeAccumulated);
        }

        return new FileManifest(
                fileManifestName,
                HEX_FORMAT.formatHex(manifestDigest.digest()),
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
