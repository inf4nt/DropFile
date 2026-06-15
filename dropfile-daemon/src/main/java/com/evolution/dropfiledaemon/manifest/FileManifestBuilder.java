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
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

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
        if (fileManifest.chunkManifests() == null || fileManifest.chunkManifests().isEmpty()) {
            throw new IllegalStateException("File manifest has no chunk manifests");
        }

        boolean zeroSizeChunk = fileManifest.chunkManifests().stream()
                .anyMatch(it -> it.size() <= 0);
        if (zeroSizeChunk) {
            throw new RuntimeException("Found zero chunks size. Chunks size must be greater than zero");
        }

        long zeroPosition = fileManifest.chunkManifests().stream()
                .filter(it -> it.position() == 0)
                .count();
        if (zeroPosition != 1) {
            throw new RuntimeException("Chunks must include one chunk with zero position");
        }

        boolean negativeStart = fileManifest.chunkManifests().stream()
                .anyMatch(it -> it.position() < 0);
        if (negativeStart) {
            throw new RuntimeException("Chunks position must be greater or equal zero");
        }

        boolean oversized = fileManifest.chunkManifests().stream()
                .anyMatch(it -> it.size() > chunkMaxSize);
        if (oversized) {
            throw new RuntimeException("File manifest has oversized chunk manifests");
        }

        long totalSizeByChunkSize = fileManifest.chunkManifests()
                .stream().map(it -> it.size())
                .collect(Collectors.summarizingLong(x -> x))
                .getSum();
        if (totalSizeByChunkSize != fileManifest.size()) {
            throw new RuntimeException("File manifest size does not match chunks size");
        }

        List<Long> positionsBasedOnChunkSize = getPositionsBasedOnChunkSize(fileManifest.chunkManifests());
        List<Long> actual = fileManifest.chunkManifests().stream().map(it -> it.position()).toList();
        if (!positionsBasedOnChunkSize.equals(actual)) {
            throw new RuntimeException("Chunk positions are invalid");
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

    private List<Long> getPositionsBasedOnChunkSize(List<ChunkManifest> chunkManifests) {
        AtomicLong offset = new AtomicLong(0);
        return chunkManifests.stream()
                .map(chunk -> offset.getAndAdd(chunk.size()))
                .toList();
    }
}
