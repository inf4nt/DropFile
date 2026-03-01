package com.evolution.dropfiledaemon.tunnel.share.dto;

import java.util.List;

@Deprecated
public record ShareDownloadManifestTunnelResponse(String id,
                                                  String hash,
                                                  long size,
                                                  List<ChunkManifest> chunkManifests) {

    @Deprecated
    public record ChunkManifest(String hash, int size, long startPosition, long endPosition) {

    }
}
