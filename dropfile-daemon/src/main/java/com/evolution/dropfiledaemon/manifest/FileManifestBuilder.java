package com.evolution.dropfiledaemon.manifest;

import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

@Component
public class FileManifestBuilder {

    private static final String SHA256 = "SHA-256";

    private static final HexFormat HEX_FORMAT = HexFormat.of();

    private final int chunkMaxSize;

    @Autowired
    public FileManifestBuilder(DaemonApplicationProperties daemonApplicationProperties) {
        this(daemonApplicationProperties.manifestChunkMaxSize);
    }

    FileManifestBuilder(int chunkMaxSize) {
        this.chunkMaxSize = chunkMaxSize;
    }

    public void validate(FileManifest fileManifest) {
        List<ChunkManifest> chunks = fileManifest.chunkManifests();

        if (chunks == null || chunks.isEmpty()) {
            throw new IllegalStateException("File manifest has no chunk manifests");
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

    public FileManifest build(Path path, String fileManifestName, int chunkSize) throws IOException, NoSuchAlgorithmException {
        if (ObjectUtils.isEmpty(fileManifestName)) {
            throw new IllegalStateException("File manifest name is empty");
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be greater than zero");
        }
        if (!Files.exists(path)) {
            throw new FileNotFoundException("No file found: " + path);
        }
        if (Files.isDirectory(path)) {
            throw new UnsupportedOperationException("Directories are unsupported: " + path.toAbsolutePath());
        }

        final long fileSize = Files.size(path);

        List<ChunkManifest> chunkManifests = new ArrayList<>();
        long totalSizeAccumulated = 0;

        MessageDigest manifestDigest = MessageDigest.getInstance(SHA256);
        MessageDigest chunkDigest = MessageDigest.getInstance(SHA256);

        OutputStream digestOutputStream = new OutputStream() {
            @Override
            public void write(int b) {
                manifestDigest.update((byte) b);
                chunkDigest.update((byte) b);
            }

            @Override
            public void write(byte[] b, int off, int len) {
                manifestDigest.update(b, off, len);
                chunkDigest.update(b, off, len);
            }
        };

        try (FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ);
             WritableByteChannel targetChannel = Channels.newChannel(digestOutputStream)) {

            long position = 0;

            while (position < fileSize) {
                int bytesInChunk = (int) Math.min(chunkSize, fileSize - position);
                long bytesTransferredInChunk = 0;

                while (bytesTransferredInChunk < bytesInChunk) {
                    long transferred = fileChannel.transferTo(
                            position + bytesTransferredInChunk,
                            bytesInChunk - bytesTransferredInChunk,
                            targetChannel
                    );

                    if (transferred <= 0) {
                        throw new IOException("Unexpected EOF or channel closed during chunk transfer");
                    }

                    bytesTransferredInChunk += transferred;
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
}
