package com.evolution.dropfiledaemon.download;

public record FileDownloadResponse(String operationId,
                                   String fileId,
                                   String filename) {
}
