package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.configuration.app.DropFileAppConfig;
import com.evolution.dropfile.configuration.crypto.CryptoUtils;
import com.evolution.dropfile.configuration.keys.DropFileKeysConfig;
import com.evolution.dropfile.configuration.secret.DropFileSecretsConfig;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStoreManager;
import com.evolution.dropfiledaemon.handshake.store.InMemoryHandshakeStoreManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.security.KeyPair;
import java.util.Base64;

@Slf4j
@Profile("dev")
@Configuration
public class DropFileDaemonConfigurationDev {

    @Bean
    public DropFileAppConfig.DropFileDaemonAppConfig appConfig(
            @Value("${dropfile.download.directory}") String downloadDirectory,
            @Value("${dropfile.daemon.port}") Integer daemonPort) {
        log.info("Provided download directory: {}", downloadDirectory);
        log.info("Provided daemon port: {}", daemonPort);
        return new DropFileAppConfig.DropFileDaemonAppConfig(downloadDirectory, daemonPort);
    }

    @Bean
    public DropFileSecretsConfig secretsConfig(@Value("${dropfile.daemon.token}") String daemonSecret) {
        log.info("Provided daemon secret: {}", daemonSecret);
        return new DropFileSecretsConfig(daemonSecret);
    }

    @Bean
    public DropFileKeysConfig keysConfig(@Value("${dropfile.public.key:#{null}}") String publicKey,
                                         @Value("${dropfile.private.key:#{null}}") String privateKey) {
        if (publicKey == null || privateKey == null) {
            KeyPair keyPair = CryptoUtils.generateKeyPair();
            log.info("Generated public key: {}", Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
            log.info("Generated private key: {}", Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
            return new DropFileKeysConfig(keyPair);
        }

        log.info("Provided public key: {}", publicKey);
        log.info("Provided private key: {}", privateKey);

        return new DropFileKeysConfig(
                new KeyPair(
                        CryptoUtils.getPublicKey(Base64.getDecoder().decode(publicKey)),
                        CryptoUtils.getPrivateKey(Base64.getDecoder().decode(privateKey))
                )
        );
    }

    @Bean
    public HandshakeStoreManager handshakeStore() {
        return new InMemoryHandshakeStoreManager();
    }
}