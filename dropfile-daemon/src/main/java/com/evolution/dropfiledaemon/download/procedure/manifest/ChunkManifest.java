package com.evolution.dropfiledaemon.download.procedure.manifest;

public record ChunkManifest(
        int size,
        long position
) {
    public ChunkManifest {
        if (size <= 0) {
            throw new IllegalArgumentException("Chunk size must be strictly positive, got: " + size);
        }
        if (position < 0) {
            throw new IllegalArgumentException("Chunk position cannot be negative, got: " + position);
        }
    }
}