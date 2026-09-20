package com.evolution.dropfiledaemon.download.procedure.manifest;

import java.util.List;
import java.util.Objects;

public record FileManifest(
        String hash,
        long size,
        List<ChunkManifest> chunks
) {
    public FileManifest {
        Objects.requireNonNull(hash, "hash must not be null");
        if (hash.isBlank()) {
            throw new IllegalArgumentException("hash must not be blank");
        }
        if (size < 0) {
            throw new IllegalArgumentException("size cannot be negative: " + size);
        }
        Objects.requireNonNull(chunks, "chunks must not be null");
    }
}