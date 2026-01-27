package com.evolution.dropfiledaemon.tunnel.share.dto;

public record ShareDownloadChunkStreamTunnelRequest(String id,
                                                    long startPosition,
                                                    long endPosition) {
}
