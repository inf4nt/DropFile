package com.evolution.dropfiledaemon.manifest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Component
public class FileManifestBuilder {

    private static final Integer CHUNK_SIZE = 5_000_000;

    private final FileHelper fileHelper;

    @Autowired
    public FileManifestBuilder(FileHelper fileHelper) {
        this.fileHelper = fileHelper;
    }

    public FileManifest build(File file) {
        if (!Files.exists(file.toPath())) {
            throw new RuntimeException("Not found: " + file.getAbsolutePath());
        }

        if (Files.isDirectory(file.toPath())) {
            throw new UnsupportedOperationException("Directory is not allowed: " + file.getAbsolutePath());
        }

        long fileLength = file.length();
        String fileName = file.getName();

        List<ChunkManifest> chunkManifests = new ArrayList<>();

        int chunkSizeFactor = getChunkSizeFactor(file);

        fileHelper.read(file, chunkSizeFactor, chunkContainer -> {
            byte[] data = chunkContainer.data();
            int chunkSize = data.length;
            String hash = fileHelper.sha256(data);
            ChunkManifest chunkManifest = new ChunkManifest(
                    chunkContainer.from(),
                    chunkContainer.to(),
                    chunkSize,
                    hash
            );
            chunkManifests.add(chunkManifest);
        });

        return new FileManifest(fileName, fileLength, chunkManifests);
    }

    private int getChunkSizeFactor(File file) {
        long size = Math.min(CHUNK_SIZE, file.length());
        return Math.toIntExact(size);
    }
}
