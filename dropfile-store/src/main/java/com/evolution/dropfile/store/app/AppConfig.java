package com.evolution.dropfile.store.app;

public record AppConfig(
        CliAppConfig cliAppConfig,
        DaemonAppConfig daemonAppConfig) {

    public record CliAppConfig(String daemonHost,
                               int daemonPort) {
    }

    public record DaemonAppConfig(String downloadDirectory,
                                  int daemonPort) {
    }
}
