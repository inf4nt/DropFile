package com.evolution.dropfile.configuration.app;

import java.net.URI;

public record DropFileAppConfig(
        DropFileCliAppConfig cliAppConfig,
        DropFileDaemonAppConfig daemonAppConfig) {

    public record DropFileCliAppConfig(String daemonHost,
                                       Integer daemonPort) {
    }

    public record DropFileDaemonAppConfig(String downloadDirectory,
                                          Integer daemonPort,
                                          URI publicDaemonAddressURI) {
    }
}
