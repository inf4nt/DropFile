package com.evolution.dropfile.configuration.app;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.net.URI;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class DropFileAppConfig {

    private DropFileCliAppConfig cliAppConfig;

    private DropFileDaemonAppConfig daemonAppConfig;

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    public static class DropFileCliAppConfig {
        private String daemonHost;

        private Integer daemonPort;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    public static class DropFileDaemonAppConfig {
        private String downloadDirectory;

        private Integer daemonPort;

        private URI publicDaemonAddressURI;
    }
}
