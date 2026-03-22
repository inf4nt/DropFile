package com.evolution.dropfiledaemon.tunnel.share.dto;

public record ShareDownloadManifestCommandRequest(String fileId,
                                                  int chunkSize) {
}
