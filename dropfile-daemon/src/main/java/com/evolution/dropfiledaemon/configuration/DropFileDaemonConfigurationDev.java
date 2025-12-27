package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoUtils;
import com.evolution.dropfile.configuration.app.AppConfig;
import com.evolution.dropfile.configuration.app.AppConfigStore;
import com.evolution.dropfile.configuration.app.ImmutableAppConfigStore;
import com.evolution.dropfile.configuration.keys.KeysConfig;
import com.evolution.dropfile.configuration.keys.KeysConfigStore;
import com.evolution.dropfile.configuration.keys.ImmutableKeysConfigStore;
import com.evolution.dropfile.configuration.secret.SecretsConfig;
import com.evolution.dropfile.configuration.secret.SecretsConfigStore;
import com.evolution.dropfile.configuration.secret.ImmutableSecretsConfigStore;
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
    public AppConfigStore appConfigStore(Environment environment) {
        return new ImmutableAppConfigStore(() -> {

            Integer daemonPort = Integer.valueOf(environment.getRequiredProperty("dropfile.daemon.port"));
            String daemonPublicAddress = environment.getProperty("dropfile.daemon.public.address");
            String daemonDownloadDirectory = environment.getRequiredProperty("dropfile.daemon.download.directory");

            log.info("Provided download directory: {}", daemonDownloadDirectory);
            log.info("Provided daemon port: {}", daemonPort);
            log.info("Provided public address: {}", daemonPublicAddress);

            return new AppConfig(
                    null,
                    new AppConfig.DaemonAppConfig(
                            daemonDownloadDirectory,
                            daemonPort,
                            Optional.ofNullable(daemonPublicAddress).map(it -> CommonUtils.toURI(it)).orElse(null)
                    )
            );
        });
    }

    @Bean
    public SecretsConfigStore secretsConfigStore(Environment environment) {
        return new ImmutableSecretsConfigStore(() -> {
            String daemonToken = environment.getRequiredProperty("dropfile.daemon.token");
            log.info("Provided daemon token: {}", daemonToken);
            return new SecretsConfig(daemonToken);
        });
    }

    @Bean
    public KeysConfigStore keysConfigStore(Environment environment) {
        return new ImmutableKeysConfigStore(() -> {
            String publicKey = environment.getProperty("dropfile.public.key");
            String privateKey = environment.getProperty("dropfile.private.key");

            if (publicKey == null || privateKey == null) {
                KeyPair keyPair = CryptoUtils.generateKeyPair();
                log.info("Generated public key: {}", CryptoUtils.encodeBase64(keyPair.getPublic().getEncoded()));
                log.info("Generated private key: {}", CryptoUtils.encodeBase64(keyPair.getPrivate().getEncoded()));

                return new KeysConfig(keyPair.getPublic().getEncoded(), keyPair.getPrivate().getEncoded());
            }

            log.info("Provided public key: {}", publicKey);
            log.info("Provided private key: {}", privateKey);

            return new KeysConfig(
                    CryptoUtils.getPublicKey(Base64.getDecoder().decode(publicKey)).getEncoded(),
                    CryptoUtils.getPrivateKey(Base64.getDecoder().decode(privateKey)).getEncoded()
            );
        });
    }
}