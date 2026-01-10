package com.evolution.dropfiledaemon.tunnel.share.dto;

public record ShareDownloadChunkTunnelRequest(String id,
                                              long startPosition,
                                              long endPosition) {
}
