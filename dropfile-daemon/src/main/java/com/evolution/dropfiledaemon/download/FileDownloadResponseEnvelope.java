package com.evolution.dropfiledaemon.download;

import java.util.List;

public record FileDownloadResponseEnvelope(List<FileDownloadResponse> responses,
                                           List<FileDownloadRequest> skipped) {
}
