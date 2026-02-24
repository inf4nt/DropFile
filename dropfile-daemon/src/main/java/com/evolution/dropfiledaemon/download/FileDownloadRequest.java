package com.evolution.dropfiledaemon.download;

public record FileDownloadRequest(String fingerprint,
                                  String fileId,
                                  String filename) {
}
