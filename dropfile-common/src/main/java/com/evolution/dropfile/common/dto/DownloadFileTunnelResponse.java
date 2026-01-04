package com.evolution.dropfile.common.dto;

public record DownloadFileTunnelResponse(String id,
                                         String alias,
                                         byte[] payload) {
}
