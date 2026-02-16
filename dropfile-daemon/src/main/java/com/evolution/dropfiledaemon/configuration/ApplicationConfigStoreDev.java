package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfile.store.access.RuntimeAccessKeyStore;
import com.evolution.dropfile.store.app.AppConfig;
import com.evolution.dropfile.store.app.AppConfigStore;
import com.evolution.dropfile.store.app.ImmutableAppConfigStore;
import com.evolution.dropfile.store.download.FileDownloadEntryStore;
import com.evolution.dropfile.store.download.RuntimeFileDownloadEntryStore;
import com.evolution.dropfile.store.secret.ImmutableSecretsConfigStore;
import com.evolution.dropfile.store.secret.SecretsConfig;
import com.evolution.dropfile.store.secret.SecretsConfigStore;
import com.evolution.dropfile.store.share.RuntimeShareFileEntryStore;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStore;
import com.evolution.dropfiledaemon.handshake.store.runtime.RuntimeHandshakeSessionStore;
import com.evolution.dropfiledaemon.handshake.store.runtime.RuntimeHandshakeTrustedInStore;
import com.evolution.dropfiledaemon.handshake.store.runtime.RuntimeHandshakeTrustedOutStore;
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

@Profile("dev")
@Slf4j
@Configuration
class ApplicationConfigStoreDev
        implements ApplicationConfigStore, AppConfigStoreUninitialized, ApplicationListener<ApplicationReadyEvent> {

    private boolean initialized = false;

    private final ApplicationEventPublisher eventPublisher;

    private final AppConfigStore appConfigStore;

    private final SecretsConfigStore secretsConfigStore;

    private final AccessKeyStore accessKeyStore;

    private final FileDownloadEntryStore fileDownloadEntryStore;

    private final ShareFileEntryStore shareFileEntryStore;

    private final HandshakeStore handshakeStore;

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

        accessKeyStore = new RuntimeAccessKeyStore();
        fileDownloadEntryStore = new RuntimeFileDownloadEntryStore();
        shareFileEntryStore = new RuntimeShareFileEntryStore();

        handshakeStore = new HandshakeStore(
                new RuntimeHandshakeTrustedOutStore(),
                new RuntimeHandshakeTrustedInStore(),
                new RuntimeHandshakeSessionStore()
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

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        initialized = true;

        eventPublisher.publishEvent(new ApplicationConfigStoreInitialized());
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
}
