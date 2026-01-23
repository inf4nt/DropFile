package com.evolution.dropfiledaemon.manifest;

import java.util.List;

public record FileManifest(String fileName, String hash, long size, List<ChunkManifest> chunkManifests) {
}
