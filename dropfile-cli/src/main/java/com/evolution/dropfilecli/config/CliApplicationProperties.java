package com.evolution.dropfilecli.config;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;

@Component
public class CliApplicationProperties {

    public final String configDirectory;

    public final String daemonHost;

    public final int daemonPort;

    public int fileOperationsBufferSize;

    @Autowired
    public CliApplicationProperties(@Value("${user.dir}") String applicationDirectory,
                                    @Value("${dropfile.daemon.host}") String daemonHost,
                                    @Value("${dropfile.daemon.port}") int daemonPort,
                                    @Value("${dropfile.file.operations.buffer-size}") int fileOperationsBufferSize) {
        this.configDirectory = getConfigDirectory(applicationDirectory);
        this.daemonHost = daemonHost;
        this.daemonPort = daemonPort;
        this.fileOperationsBufferSize = fileOperationsBufferSize;
    }

    @SneakyThrows
    private String getConfigDirectory(String applicationDirectory) {
        return Paths.get(applicationDirectory, "conf").toString();
    }
}
