package com.evolution.dropfiledaemon.tunnel.share.dto;

public record ShareDownloadChunkStreamTunnelRequest(String id,
                                                    int size,
                                                    long position) {
}
