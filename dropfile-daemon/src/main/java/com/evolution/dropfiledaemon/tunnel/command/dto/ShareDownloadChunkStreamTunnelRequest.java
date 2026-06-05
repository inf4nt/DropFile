package com.evolution.dropfiledaemon.tunnel.command.dto;

public record ShareDownloadChunkStreamTunnelRequest(String id,
                                                    int size,
                                                    long position) {
}
