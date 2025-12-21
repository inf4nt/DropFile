package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoUtils;
import com.evolution.dropfile.configuration.app.DropFileAppConfig;
import com.evolution.dropfile.configuration.app.DropFileAppConfigStore;
import com.evolution.dropfile.configuration.app.ImmutableDropFileAppConfigStore;
import com.evolution.dropfile.configuration.keys.DropFileKeysConfig;
import com.evolution.dropfile.configuration.keys.DropFileKeysConfigStore;
import com.evolution.dropfile.configuration.keys.ImmutableDropFileKeysConfigStore;
import com.evolution.dropfile.configuration.secret.DropFileSecretsConfig;
import com.evolution.dropfile.configuration.secret.DropFileSecretsConfigStore;
import com.evolution.dropfile.configuration.secret.ImmutableDropFileSecretsConfigStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.security.KeyPair;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Profile("dev")
@Configuration
public class DropFileDaemonConfigurationDev {

    @Bean
    public DropFileAppConfigStore appConfigStore(Environment environment) {
        return new ImmutableDropFileAppConfigStore(() -> {

            Integer daemonPort = Integer.valueOf(environment.getRequiredProperty("dropfile.daemon.port"));
            String daemonPublicAddress = environment.getProperty("dropfile.daemon.public.address");
            String daemonDownloadDirectory = environment.getRequiredProperty("dropfile.daemon.download.directory");

            log.info("Provided download directory: {}", daemonDownloadDirectory);
            log.info("Provided daemon port: {}", daemonPort);
            log.info("Provided public address: {}", daemonPublicAddress);

            return new DropFileAppConfig(
                    null,
                    new DropFileAppConfig.DropFileDaemonAppConfig(
                            daemonDownloadDirectory,
                            daemonPort,
                            Optional.ofNullable(daemonPublicAddress).map(it -> CommonUtils.toURI(it)).orElse(null)
                    )
            );
        });
    }

    @Bean
    public DropFileSecretsConfigStore secretsConfigStore(Environment environment) {
        return new ImmutableDropFileSecretsConfigStore(() -> {
            String daemonToken = environment.getRequiredProperty("dropfile.daemon.token");
            log.info("Provided daemon token: {}", daemonToken);
            return new DropFileSecretsConfig(daemonToken);
        });
    }

    @Bean
    public DropFileKeysConfigStore keysConfigStore(Environment environment) {
        return new ImmutableDropFileKeysConfigStore(() -> {
            String publicKey = environment.getProperty("dropfile.public.key");
            String privateKey = environment.getProperty("dropfile.private.key");

            if (publicKey == null || privateKey == null) {
                KeyPair keyPair = CryptoUtils.generateKeyPair();
                log.info("Generated public key: {}", CryptoUtils.encodeBase64(keyPair.getPublic().getEncoded()));
                log.info("Generated private key: {}", CryptoUtils.encodeBase64(keyPair.getPrivate().getEncoded()));
                return new DropFileKeysConfig(keyPair.getPublic().getEncoded(), keyPair.getPrivate().getEncoded());
            }

            log.info("Provided public key: {}", publicKey);
            log.info("Provided private key: {}", privateKey);

            return new DropFileKeysConfig(
                    CryptoUtils.getPublicKey(Base64.getDecoder().decode(publicKey)).getEncoded(),
                    CryptoUtils.getPrivateKey(Base64.getDecoder().decode(privateKey)).getEncoded()
            );
        });
    }
}