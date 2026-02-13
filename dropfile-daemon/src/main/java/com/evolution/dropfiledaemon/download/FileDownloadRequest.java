package com.evolution.dropfiledaemon.download;

public record FileDownloadRequest(String fingerprintConnection,
                                  String fileId,
                                  String filename) {
}
