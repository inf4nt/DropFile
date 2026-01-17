package com.evolution.dropfiledaemon.tunnel.share.dto;

import java.util.List;

public record ShareDownloadManifestResponse(String id, long size, String hash, List<ChunkManifest> chunkManifests) {

    public record ChunkManifest(long startPosition, long endPosition, int size, String hash) {

    }
}
