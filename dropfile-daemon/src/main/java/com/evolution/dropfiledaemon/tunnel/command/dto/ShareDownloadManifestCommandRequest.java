package com.evolution.dropfiledaemon.tunnel.command.dto;

public record ShareDownloadManifestCommandRequest(String fileId,
                                                  int chunkSize) {
}
