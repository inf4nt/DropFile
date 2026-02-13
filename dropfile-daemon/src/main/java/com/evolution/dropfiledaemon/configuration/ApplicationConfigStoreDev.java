package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.common.crypto.CryptoRSA;
import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfile.store.access.RuntimeAccessKeyStore;
import com.evolution.dropfile.store.app.AppConfig;
import com.evolution.dropfile.store.app.AppConfigStore;
import com.evolution.dropfile.store.app.ImmutableAppConfigStore;
import com.evolution.dropfile.store.download.FileDownloadEntryStore;
import com.evolution.dropfile.store.download.RuntimeFileDownloadEntryStore;
import com.evolution.dropfile.store.keys.ImmutableKeysConfigStore;
import com.evolution.dropfile.store.keys.KeysConfig;
import com.evolution.dropfile.store.keys.KeysConfigStore;
import com.evolution.dropfile.store.secret.ImmutableSecretsConfigStore;
import com.evolution.dropfile.store.secret.SecretsConfig;
import com.evolution.dropfile.store.secret.SecretsConfigStore;
import com.evolution.dropfile.store.share.RuntimeShareFileEntryStore;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStore;
import com.evolution.dropfiledaemon.handshake.store.runtime.RuntimeTrustedInKeyValueStore;
import com.evolution.dropfiledaemon.handshake.store.runtime.RuntimeTrustedOutKeyValueStore;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;

@Profile("dev")
@Slf4j
@Configuration
public class ApplicationConfigStoreDev
        implements ApplicationConfigStore, AppConfigStoreUninitialized, ApplicationListener<ApplicationReadyEvent> {

    private boolean initialized = false;

    private final AppConfigStore appConfigStore;

    private final SecretsConfigStore secretsConfigStore;

    private final KeysConfigStore keysConfigStore;

    private final AccessKeyStore accessKeyStore;

    private final FileDownloadEntryStore fileDownloadEntryStore;

    private final ShareFileEntryStore shareFileEntryStore;

    private final HandshakeStore handshakeStore;

    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public ApplicationConfigStoreDev(Environment environment, ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;

        appConfigStore = new ImmutableAppConfigStore(() -> {
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

        secretsConfigStore = new ImmutableSecretsConfigStore(() -> {
            String daemonToken = environment.getRequiredProperty("dropfile.daemon.token");
            log.info("Provided daemon token: {}", daemonToken);
            return new SecretsConfig(daemonToken);
        });

        keysConfigStore = new ImmutableKeysConfigStore(() -> {
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

        accessKeyStore = new RuntimeAccessKeyStore();
        fileDownloadEntryStore = new RuntimeFileDownloadEntryStore();
        shareFileEntryStore = new RuntimeShareFileEntryStore();
        handshakeStore = new HandshakeStore(
                new RuntimeTrustedInKeyValueStore(),
                new RuntimeTrustedOutKeyValueStore()
        );
    }

    @Override
    public AppConfigStore getAppConfigStore() {
        checkInitialized();
        return appConfigStore;
    }

    @Override
    public AppConfigStore getUninitializedAppConfigStore() {
        return appConfigStore;
    }

    @Override
    public KeysConfigStore getKeysConfigStore() {
        checkInitialized();
        return keysConfigStore;
    }

    @Override
    public AccessKeyStore getAccessKeyStore() {
        checkInitialized();
        return accessKeyStore;
    }

    @Override
    public FileDownloadEntryStore getFileDownloadEntryStore() {
        checkInitialized();
        return fileDownloadEntryStore;
    }

    @Override
    public SecretsConfigStore getSecretsConfigStore() {
        checkInitialized();
        return secretsConfigStore;
    }

    @Override
    public ShareFileEntryStore getShareFileEntryStore() {
        checkInitialized();
        return shareFileEntryStore;
    }

    @Override
    public HandshakeStore getHandshakeStore() {
        checkInitialized();
        return handshakeStore;
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

    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Application has not been initialized yet");
        }
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        initialized = true;

        eventPublisher.publishEvent(new ApplicationConfigStoreInitialized());
    }
}
