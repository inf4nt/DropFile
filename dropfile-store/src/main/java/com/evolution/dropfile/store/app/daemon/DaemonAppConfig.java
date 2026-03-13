package com.evolution.dropfile.store.app.daemon;

public record DaemonAppConfig(String downloadDirectory,
                              int daemonPort,
                              int downloadOrchestratorThreadSize,
                              int downloadProcedureThreadSize,
                              int downloadProcedureManifestCallTimeoutMillis,
                              int downloadProcedureChunkCallTimeoutMillis,
                              boolean compressTunnelActive,
                              int compressTunnelLevel) {
}
