package com.evolution.dropfile.configuration.app;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class DropFileAppConfig {

    private DropFileCliAppConfig dropFileCliAppConfig;

    private DropFileDaemonAppConfig dropFileDaemonAppConfig;

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
    }
}
