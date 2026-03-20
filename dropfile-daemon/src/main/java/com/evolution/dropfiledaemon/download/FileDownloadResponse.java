package com.evolution.dropfiledaemon.download;

public record FileDownloadResponse(String operationId,
                                   String fingerprint,
                                   String fileId,
                                   String filename) {
}
