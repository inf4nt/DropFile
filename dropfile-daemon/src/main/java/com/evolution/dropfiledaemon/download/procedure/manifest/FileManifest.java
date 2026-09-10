package com.evolution.dropfiledaemon.download.procedure.manifest;

import java.util.List;

public record FileManifest(String hash,
                           long size,
                           List<ChunkManifest> chunks) {
}
