package com.evolution.dropfiledaemon.download.procedure;

public record DownloadProcedureConfiguration(int maxThreadSize,
                                             int manifestCallTimeoutMillis,
                                             int chunkCallTimeoutMillis) {
}
