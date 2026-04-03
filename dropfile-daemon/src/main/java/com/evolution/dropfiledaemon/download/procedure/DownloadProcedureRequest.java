package com.evolution.dropfiledaemon.download.procedure;

import java.nio.file.Path;

public record DownloadProcedureRequest(String operation,
                                       String fingerprint,
                                       String fileId,
                                       String filename,
                                       Path destinationFilePath,
                                       Path temporaryFilePath,
                                       Path manifestFilePath) {
}
