package com.evolution.dropfiledaemon.tunnel.command.dto;

import com.evolution.dropfiledaemon.manifest.FileManifest;

public record ShareDownloadManifestCommandResponse(String fileId,
                                                   FileManifest fileManifest) {
}
