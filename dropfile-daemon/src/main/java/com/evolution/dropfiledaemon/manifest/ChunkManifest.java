package com.evolution.dropfiledaemon.manifest;

public record ChunkManifest(String hash, int size, long position) {
}
