package com.evolution.dropfiledaemon.tunnel.share.dto;

import java.util.List;

public record ShareDownloadManifestTunnelResponse(String id,
                                                  String hash,
                                                  long size,
                                                  List<ChunkManifest> chunkManifests) {

    public record ChunkManifest(String hash, int size, long startPosition, long endPosition) {

    }
}
