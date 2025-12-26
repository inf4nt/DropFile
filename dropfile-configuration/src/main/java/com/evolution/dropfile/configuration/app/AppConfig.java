package com.evolution.dropfile.configuration.app;

import java.net.URI;

public record AppConfig(
        CliAppConfig cliAppConfig,
        DaemonAppConfig daemonAppConfig) {

    public record CliAppConfig(String daemonHost,
                               Integer daemonPort) {
    }

    public record DaemonAppConfig(String downloadDirectory,
                                  Integer daemonPort,
                                  URI publicDaemonAddressURI) {
    }
}
