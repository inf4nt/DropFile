package com.evolution.dropfiledaemon.download.procedure;

import java.io.File;

public record DownloadProcedureRequest(String operation,
                                       String fingerprint,
                                       String fileId,
                                       String filename,
                                       File destinationFile,
                                       File temporaryFile) {
}
