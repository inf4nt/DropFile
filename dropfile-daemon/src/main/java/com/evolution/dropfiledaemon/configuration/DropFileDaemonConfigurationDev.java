package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.common.crypto.CryptoRSA;
import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfile.store.access.RuntimeAccessKeyStore;
import com.evolution.dropfile.store.app.AppConfig;
import com.evolution.dropfile.store.app.AppConfigStore;
import com.evolution.dropfile.store.app.ImmutableAppConfigStore;
import com.evolution.dropfile.store.download.DownloadFileEntryStore;
import com.evolution.dropfile.store.download.RuntimeDownloadFileEntryStore;
import com.evolution.dropfile.store.keys.ImmutableKeysConfigStore;
import com.evolution.dropfile.store.keys.KeysConfig;
import com.evolution.dropfile.store.keys.KeysConfigStore;
import com.evolution.dropfile.store.secret.ImmutableSecretsConfigStore;
import com.evolution.dropfile.store.secret.SecretsConfig;
import com.evolution.dropfile.store.secret.SecretsConfigStore;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;

@Slf4j
@Profile("dev")
@Configuration
public class DropFileDaemonConfigurationDev {

    @Bean
    public AppConfigStore appConfigStore(Environment environment) {
        return new ImmutableAppConfigStore(() -> {
            Integer daemonPort = Integer.valueOf(environment.getRequiredProperty("dropfile.daemon.port"));
            String daemonDownloadDirectory = environment.getProperty("dropfile.daemon.download.directory");

            log.info("Provided download directory: {}", daemonDownloadDirectory);
            log.info("Provided daemon port: {}", daemonPort);

            File daemonDownloadDirectoryFile = getDaemonDownloadDirectory(daemonDownloadDirectory);
            log.info("Download directory: {}", daemonDownloadDirectoryFile.getAbsolutePath());

            return new AppConfig(
                    null,
                    new AppConfig.DaemonAppConfig(
                            daemonDownloadDirectoryFile.getAbsolutePath(),
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
    public KeysConfigStore keysConfigStore() {
        return new ImmutableKeysConfigStore(() -> {
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
        });
    }

    @Bean
    public AccessKeyStore accessKeyStore() {
        return new RuntimeAccessKeyStore();
    }

    @Bean
    public DownloadFileEntryStore downloadFileEntryStore() {
        return new RuntimeDownloadFileEntryStore();
    }

    @SneakyThrows
    private File getDaemonDownloadDirectory(String daemonDownloadDirectory) {
        if (daemonDownloadDirectory != null && !daemonDownloadDirectory.isBlank()) {
            Path daemonDownloadDirectoryPath = Paths.get(daemonDownloadDirectory)
                    .normalize();
            if (!daemonDownloadDirectoryPath.isAbsolute()) {
                throw new IllegalArgumentException("Daemon download directory has to be absolute: " + daemonDownloadDirectoryPath);
            }
            if (Files.notExists(daemonDownloadDirectoryPath)) {
                throw new FileNotFoundException("Daemon download directory does not exist: " + daemonDownloadDirectoryPath);
            }
            return daemonDownloadDirectoryPath.toFile();
        }
        Path downloadDirectoryPath = Paths.get(System.getProperty("user.home"), ".dropfile");
        if (Files.notExists(downloadDirectoryPath)) {
            Files.createDirectory(downloadDirectoryPath);
        }
        return downloadDirectoryPath.toFile();
    }
}