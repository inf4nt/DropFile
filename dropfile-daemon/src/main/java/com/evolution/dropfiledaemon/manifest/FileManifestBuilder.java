package com.evolution.dropfiledaemon.manifest;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

@Component
public class FileManifestBuilder {

    private static final String SHA256 = "SHA-256";

    private static final Integer CHUNK_SIZE = 1 * 1024 * 1024;

    private final FileHelper fileHelper;

    @Autowired
    public FileManifestBuilder(FileHelper fileHelper) {
        this.fileHelper = fileHelper;
    }

    @SneakyThrows
    public FileManifest build(File file) {
        if (!Files.exists(file.toPath())) {
            throw new FileNotFoundException("No file found: " + file.getAbsolutePath());
        }

        if (Files.isDirectory(file.toPath())) {
            throw new UnsupportedOperationException("Directory is not allowed: " + file.getAbsolutePath());
        }

        long fileLength = file.length();
        String fileName = file.getName();

        List<ChunkManifest> chunkManifests = new ArrayList<>();

        int chunkSizeFactor = getChunkSizeFactor(file);

        MessageDigest manifestMessageDigest = MessageDigest.getInstance(SHA256);

        fileHelper.read(file, chunkSizeFactor, chunkContainer -> {
            byte[] data = chunkContainer.data();
            int chunkSize = data.length;
            String hash = fileHelper.sha256(data);
            manifestMessageDigest.update(data);
            ChunkManifest chunkManifest = new ChunkManifest(
                    chunkContainer.from(),
                    chunkContainer.to(),
                    chunkSize,
                    hash
            );
            chunkManifests.add(chunkManifest);
        });

        String manifestSha256 = fileHelper.bytesToHex(manifestMessageDigest.digest());

        return new FileManifest(fileName, fileLength, manifestSha256, chunkManifests);
    }

    private int getChunkSizeFactor(File file) {
        long size = Math.min(CHUNK_SIZE, file.length());
        return Math.toIntExact(size);
    }
}
