package com.evolution.dropfiledaemon.download.procedure.manifest;

import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FileManifestService {

    private final int daemonManifestChunkMaxSize;

    @Autowired
    public FileManifestService(DaemonApplicationProperties daemonApplicationProperties) {
        this(daemonApplicationProperties.daemonManifestChunkMaxSize);
    }

    public FileManifestService(int daemonManifestChunkMaxSize) {
        if (daemonManifestChunkMaxSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be greater than zero");
        }
        this.daemonManifestChunkMaxSize = daemonManifestChunkMaxSize;
    }

    public FileManifest build(String fileHash, long fileSize) {
        List<ChunkManifest> chunks = new ArrayList<>();

        long position = 0;
        while (position < fileSize) {
            int bytesInChunk = (int) Math.min(daemonManifestChunkMaxSize, fileSize - position);
            chunks.add(new ChunkManifest(bytesInChunk, position));
            position += bytesInChunk;
        }

        return new FileManifest(
                fileHash,
                fileSize,
                chunks
        );
    }
}
