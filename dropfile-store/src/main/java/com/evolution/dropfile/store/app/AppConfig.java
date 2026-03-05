package com.evolution.dropfile.store.app;

public record AppConfig(
        CliAppConfig cliAppConfig,
        DaemonAppConfig daemonAppConfig) {

    public record CliAppConfig(String daemonHost,
                               int daemonPort) {
    }

    // TODO create separate config for daemon and cli
    public record DaemonAppConfig(String downloadDirectory,
                                  int daemonPort,
                                  Integer downloadOrchestratorThreadSize,
                                  Integer downloadProcedureThreadSize) {
    }
}
