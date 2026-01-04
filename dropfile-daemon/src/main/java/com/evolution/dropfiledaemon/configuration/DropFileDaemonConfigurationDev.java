package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.common.crypto.CryptoRSA;
import com.evolution.dropfile.configuration.app.AppConfig;
import com.evolution.dropfile.configuration.app.AppConfigStore;
import com.evolution.dropfile.configuration.app.ImmutableAppConfigStore;
import com.evolution.dropfile.configuration.keys.ImmutableKeysConfigStore;
import com.evolution.dropfile.configuration.keys.KeysConfig;
import com.evolution.dropfile.configuration.keys.KeysConfigStore;
import com.evolution.dropfile.configuration.secret.ImmutableSecretsConfigStore;
import com.evolution.dropfile.configuration.secret.SecretsConfig;
import com.evolution.dropfile.configuration.secret.SecretsConfigStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.security.KeyPair;

@Slf4j
@Profile("dev")
@Configuration
public class DropFileDaemonConfigurationDev {

    @Bean
    public AppConfigStore appConfigStore(Environment environment) {
        return new ImmutableAppConfigStore(() -> {

            Integer daemonPort = Integer.valueOf(environment.getRequiredProperty("dropfile.daemon.port"));
            String daemonDownloadDirectory = environment.getRequiredProperty("dropfile.daemon.download.directory");

            log.info("Provided download directory: {}", daemonDownloadDirectory);
            log.info("Provided daemon port: {}", daemonPort);

            return new AppConfig(
                    null,
                    new AppConfig.DaemonAppConfig(
                            daemonDownloadDirectory,
                            daemonPort
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
            String publicKeyRSA = environment.getProperty("dropfile.publicrsa.key");
            String privateKeyRSA = environment.getProperty("dropfile.privatersa.key");
            String publicKeyDH = environment.getProperty("dropfile.publicdh.key");
            String privateKeyDH = environment.getProperty("dropfile.privatedh.key");

            if (publicKeyRSA == null || privateKeyRSA == null || publicKeyDH == null || privateKeyDH == null) {
                KeyPair keyPairRSA = CryptoRSA.generateKeyPair();
                KeyPair keyPairDH = CryptoECDH.generateKeyPair();

                log.info("Generated RSA public key: {}", CommonUtils.encodeBase64(keyPairRSA.getPublic().getEncoded()));
                log.info("Generated RSA private key: {}", CommonUtils.encodeBase64(keyPairRSA.getPrivate().getEncoded()));

                log.info("Generated DH public key: {}", CommonUtils.encodeBase64(keyPairDH.getPublic().getEncoded()));
                log.info("Generated DH private key: {}", CommonUtils.encodeBase64(keyPairDH.getPrivate().getEncoded()));

                log.info("Generated fingerprint RSA public key: {}", CommonUtils.getFingerprint(keyPairRSA.getPublic()));
                log.info("Generated fingerprint DH public key: {}", CommonUtils.getFingerprint(keyPairDH.getPublic()));

                return new KeysConfig(
                        new KeysConfig.Keys(
                                keyPairRSA.getPublic().getEncoded(),
                                keyPairRSA.getPrivate().getEncoded()
                        ),
                        new KeysConfig.Keys(
                                keyPairDH.getPublic().getEncoded(),
                                keyPairDH.getPrivate().getEncoded()
                        )
                );
            }

            log.info("Provided public RSA key: {}", publicKeyRSA);
            log.info("Provided private RSA key: {}", privateKeyRSA);
            log.info("Provided public DH key: {}", publicKeyDH);
            log.info("Provided private DH key: {}", privateKeyDH);

            return new KeysConfig(
                    new KeysConfig.Keys(
                            CryptoRSA.getPublicKey(CommonUtils.decodeBase64(publicKeyRSA)).getEncoded(),
                            CryptoRSA.getPrivateKey(CommonUtils.decodeBase64(privateKeyRSA)).getEncoded()
                    ),
                    new KeysConfig.Keys(
                            CryptoECDH.getPublicKey(CommonUtils.decodeBase64(publicKeyDH)).getEncoded(),
                            CryptoECDH.getPrivateKey(CommonUtils.decodeBase64(privateKeyDH)).getEncoded()
                    )
            );
        });
    }
}