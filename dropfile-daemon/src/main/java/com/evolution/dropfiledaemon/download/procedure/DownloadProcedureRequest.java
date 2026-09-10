package com.evolution.dropfiledaemon.download.procedure;

import com.evolution.dropfiledaemon.download.procedure.manifest.FileManifest;

import java.nio.file.Path;

public record DownloadProcedureRequest(String operation,
                                       String fingerprint,
                                       String fileId,
                                       String filename,
                                       FileManifest fileManifest,
                                       Path destinationFilePath,
                                       Path temporaryFilePath,
                                       Path manifestFilePath) {
}
