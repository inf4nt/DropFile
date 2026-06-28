package com.evolution.dropfilecli.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CliApplicationProperties {

    public final String applicationDirectory;

    public final String daemonHost;

    public final int daemonPort;


    @Autowired
    public CliApplicationProperties(@Value("${user.dir}") String applicationDirectory,
                                    @Value("${dropfile.daemon.host}") String daemonHost,
                                    @Value("${dropfile.daemon.port}") int daemonPort) {
        this.applicationDirectory = applicationDirectory;
        this.daemonHost = daemonHost;
        this.daemonPort = daemonPort;
    }
}
