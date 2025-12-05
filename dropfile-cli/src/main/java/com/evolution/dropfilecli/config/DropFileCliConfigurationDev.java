package com.evolution.dropfilecli.config;

import com.evolution.dropfile.configuration.app.DropFileAppConfig;
import com.evolution.dropfile.configuration.secret.DropFileSecretsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("dev")
@Configuration
public class DropFileCliConfigurationDev {

    @Bean
    public DropFileAppConfig appConfig(@Value("${dropfile.download.directory}") String downloadDirectory,
                                       @Value("${dropfile.daemon.address}") String daemonAddress) {
        return new DropFileAppConfig(downloadDirectory, daemonAddress);
    }

    @Bean
    public DropFileSecretsConfig secretsConfig(@Value("${dropfile.daemon.token}") String daemonSecret) {
        return new DropFileSecretsConfig(daemonSecret);
    }
}
