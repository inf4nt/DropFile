package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.configuration.app.DropFileAppConfig;
import com.evolution.dropfile.configuration.keys.DropFileKeysConfigManager;
import com.evolution.dropfile.configuration.secret.DropFileSecretsConfig;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStoreManager;
import com.evolution.dropfiledaemon.handshake.store.InMemoryHandshakeStoreManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("dev")
@Configuration
public class DropFileDaemonConfigurationDev {

    @Bean
    public DropFileAppConfig appConfig(@Value("${dropfile.download.directory}") String downloadDirectory,
                                       @Value("${dropfile.daemon.address}") String daemonAddress) {
        return new DropFileAppConfig(downloadDirectory, daemonAddress);
    }

    @Bean
    public DropFileSecretsConfig secretsConfig(@Value("${dropfile.daemon.token}") String daemonSecret) {
        return new DropFileSecretsConfig(daemonSecret);
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