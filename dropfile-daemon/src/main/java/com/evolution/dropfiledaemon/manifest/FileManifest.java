package com.evolution.dropfiledaemon.manifest;

import java.util.List;

public record FileManifest(String fileName, long size, String hash, List<ChunkManifest> chunkManifests) {
}
