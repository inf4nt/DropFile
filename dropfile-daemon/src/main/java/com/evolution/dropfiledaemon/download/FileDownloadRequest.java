package com.evolution.dropfiledaemon.download;

import lombok.With;

@With
public record FileDownloadRequest(String fingerprint,
                                  String fileId,
                                  String filename,
                                  long size,
                                  String hash) {
}
