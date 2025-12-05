package com.evolution.dropfilecli.config;

import com.evolution.dropfile.configuration.app.DropFileAppConfig;
import com.evolution.dropfile.configuration.app.DropFileAppConfigManager;
import com.evolution.dropfile.configuration.secret.DropFileSecretsConfig;
import com.evolution.dropfile.configuration.secret.DropFileSecretsConfigManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("prod")
@Configuration
public class DropFileCliConfigurationProd {

    @Bean
    public DropFileAppConfigManager appConfigManager(ObjectMapper objectMapper) {
        return new DropFileAppConfigManager(objectMapper);
    }

    @Bean
    public DropFileSecretsConfigManager secretsConfigManager(ObjectMapper objectMapper) {
        return new DropFileSecretsConfigManager(objectMapper);
    }

    @Bean
    public DropFileAppConfig appConfig(DropFileAppConfigManager appConfig) {
        return appConfig.get();
    }

    @Bean
    public DropFileSecretsConfig secretsConfig(DropFileSecretsConfigManager secretsConfig) {
        return secretsConfig.get();
    }
}
