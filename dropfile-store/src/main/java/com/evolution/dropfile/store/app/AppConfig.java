package com.evolution.dropfile.store.app;

public record AppConfig(
        CliAppConfig cliAppConfig,
        DaemonAppConfig daemonAppConfig) {

    public record CliAppConfig(String daemonHost,
                               Integer daemonPort) {
    }

    public record DaemonAppConfig(String downloadDirectory,
                                  Integer daemonPort) {
    }
}
