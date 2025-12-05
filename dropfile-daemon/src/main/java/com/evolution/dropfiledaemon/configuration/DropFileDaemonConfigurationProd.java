package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.configuration.app.DropFileAppConfig;
import com.evolution.dropfile.configuration.app.DropFileAppConfigManager;
import com.evolution.dropfile.configuration.keys.DropFileKeysConfigManager;
import com.evolution.dropfile.configuration.secret.DropFileSecretsConfig;
import com.evolution.dropfile.configuration.secret.DropFileSecretsConfigManager;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStoreManager;
import com.evolution.dropfiledaemon.handshake.store.InMemoryHandshakeStoreManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("prod")
@Configuration
public class DropFileDaemonConfigurationProd {

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
    public DropFileSecretsConfig secretsConfig(DropFileSecretsConfigManager configManager) {
        configManager.refreshDaemonToken();
        return configManager.get();
    }

    @Bean
    public DropFileKeysConfigManager keysConfigManager() {
        return new DropFileKeysConfigManager();
    }

    @Bean
    public HandshakeStoreManager handshakeStore() {
        return new InMemoryHandshakeStoreManager();
    }
}