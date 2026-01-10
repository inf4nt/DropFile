package com.evolution.dropfiledaemon.manifest;

public record ChunkManifest(long startPosition, long endPosition, int size, String hash) {
}
