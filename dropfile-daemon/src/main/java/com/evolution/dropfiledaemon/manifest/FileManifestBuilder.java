package com.evolution.dropfiledaemon.manifest;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
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
            throw new UnsupportedOperationException("Directories are unsupported: " + file.getAbsolutePath());
        }

        List<ChunkManifest> chunkManifests = Collections.synchronizedList(new ArrayList<>());
        int chunkSizeFactor = getChunkSizeFactor(file);
        MessageDigest manifestMessageDigest = MessageDigest.getInstance(SHA256);

        fileHelper.read(file, chunkSizeFactor, chunkContainer -> {
            byte[] data = chunkContainer.data();
            int chunkSize = data.length;
            String sha256 = fileHelper.sha256(data);
            manifestMessageDigest.update(data);
            ChunkManifest chunkManifest = new ChunkManifest(
                    sha256,
                    chunkSize,
                    chunkContainer.from(),
                    chunkContainer.to()
            );
            chunkManifests.add(chunkManifest);
        });

        long totalSize = chunkManifests.stream().map(it -> it.size())
                .mapToLong(Long::valueOf)
                .sum();

        String manifestSha256 = fileHelper.bytesToHex(manifestMessageDigest.digest());

        return new FileManifest(file.getName(), manifestSha256, totalSize, chunkManifests);
    }

    private int getChunkSizeFactor(File file) {
        long size = Math.min(CHUNK_SIZE, file.length());
        return Math.toIntExact(size);
    }
}
