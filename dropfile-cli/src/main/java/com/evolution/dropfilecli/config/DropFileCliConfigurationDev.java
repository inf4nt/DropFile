package com.evolution.dropfilecli.config;

import com.evolution.dropfile.configuration.app.DropFileAppConfig;
import com.evolution.dropfile.configuration.secret.DropFileSecretsConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@Profile("dev")
@Configuration
public class DropFileCliConfigurationDev {

    @Bean
    public DropFileAppConfig appConfig(@Value("${dropfile.download.directory}") String downloadDirectory,
                                       @Value("${dropfile.daemon.host}") String daemonHost,
                                       @Value("${dropfile.daemon.port}") Integer daemonPort) {
        log.info("Provided download directory: {}", downloadDirectory);
        log.info("Provided daemon host: {}", daemonHost);
        log.info("Provided daemon port: {}", daemonPort);
        return new DropFileAppConfig(downloadDirectory, daemonHost, daemonPort);
    }

    @Bean
    public DropFileSecretsConfig secretsConfig(@Value("${dropfile.daemon.token}") String daemonSecret) {
        log.info("Provided daemon secret: {}", daemonSecret);
        return new DropFileSecretsConfig(daemonSecret);
    }
}
