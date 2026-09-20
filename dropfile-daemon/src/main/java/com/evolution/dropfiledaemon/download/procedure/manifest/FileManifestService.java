package com.evolution.dropfiledaemon.download.procedure.manifest;

import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

@Component
public class FileManifestService {

    // TODO add env var
    public static final long MAX_SUPPORTED_FILE_SIZE = 100L * 1024 * 1024 * 1024; // 100 GB

    private final int daemonTunnelClientManifestChunkSize;

    @Autowired
    public FileManifestService(DaemonApplicationProperties daemonApplicationProperties) {
        this(daemonApplicationProperties.daemonTunnelClientManifestChunkSize);
    }

    public FileManifestService(int daemonTunnelClientManifestChunkSize) {
        if (daemonTunnelClientManifestChunkSize <= 0) {
            throw new IllegalArgumentException(
                    "daemonTunnelClientManifestChunkSize must be strictly greater than zero, got: "
                            + daemonTunnelClientManifestChunkSize
            );
        }
        this.daemonTunnelClientManifestChunkSize = daemonTunnelClientManifestChunkSize;
    }

    public FileManifest build(String fileHash, long fileSize) {
        if (fileSize < 0) {
            throw new IllegalArgumentException("fileSize cannot be negative: " + fileSize);
        }
        if (fileSize > MAX_SUPPORTED_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "fileSize exceeds maximum supported limit %d".formatted(MAX_SUPPORTED_FILE_SIZE)
            );
        }

        if (fileSize == 0) {
            return new FileManifest(fileHash, 0, Collections.emptyList());
        }

        long chunkSize = this.daemonTunnelClientManifestChunkSize;

        long totalChunksLong = (fileSize + chunkSize - 1) / chunkSize;

        if (totalChunksLong > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("There are more chunks than Integer.MAX_VALUE for List structure");
        }

        int totalChunks = (int) totalChunksLong;

        List<ChunkManifest> lazyChunks = new AbstractList<>() {
            @Override
            public ChunkManifest get(int index) {
                if (index < 0 || index >= totalChunks) {
                    throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + totalChunks);
                }

                long position = index * chunkSize;
                int bytesInChunk = (index == totalChunks - 1)
                        ? (int) (fileSize - position)
                        : (int) chunkSize;

                return new ChunkManifest(bytesInChunk, position);
            }

            @Override
            public int size() {
                return totalChunks;
            }
        };

        return new FileManifest(
                fileHash,
                fileSize,
                Collections.unmodifiableList(lazyChunks)
        );
    }
}