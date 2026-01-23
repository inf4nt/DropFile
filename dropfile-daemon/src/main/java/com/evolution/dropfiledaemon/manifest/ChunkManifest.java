package com.evolution.dropfiledaemon.manifest;

public record ChunkManifest(String hash, int size, long startPosition, long endPosition) {
}
