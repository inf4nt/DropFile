package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfile.store.access.RuntimeAccessKeyStore;
import com.evolution.dropfile.store.app.AppConfig;
import com.evolution.dropfile.store.app.AppConfigStore;
import com.evolution.dropfile.store.app.ImmutableAppConfigStore;
import com.evolution.dropfile.store.download.FileDownloadEntryStore;
import com.evolution.dropfile.store.download.RuntimeFileDownloadEntryStore;
import com.evolution.dropfile.store.framework.KeyValueStore;
import com.evolution.dropfile.store.framework.single.SingleValueStore;
import com.evolution.dropfile.store.secret.DaemonSecrets;
import com.evolution.dropfile.store.secret.DaemonSecretsStore;
import com.evolution.dropfile.store.secret.ImmutableDaemonSecretsStore;
import com.evolution.dropfile.store.share.RuntimeShareFileEntryStore;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionInStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionOutStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.handshake.store.runtime.RuntimeHandshakeSessionInStore;
import com.evolution.dropfiledaemon.handshake.store.runtime.RuntimeHandshakeSessionOutStore;
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
import java.util.Map;

@Profile("dev")
@Slf4j
@Configuration
class ApplicationConfigStoreDev
        implements ApplicationConfigStore, AppConfigStoreUninitialized, ApplicationListener<ApplicationReadyEvent> {

    private boolean initialized = false;

    private final ApplicationEventPublisher eventPublisher;

    private final Map<Class, KeyValueStore> keyValueStores;

    private final Map<Class, SingleValueStore> singleValueStores;

    @Autowired
    public ApplicationConfigStoreDev(Environment environment, ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;

        keyValueStores = Map.of(
                AccessKeyStore.class, new RuntimeAccessKeyStore(),
                FileDownloadEntryStore.class, new RuntimeFileDownloadEntryStore(),
                ShareFileEntryStore.class, new RuntimeShareFileEntryStore(),
                HandshakeTrustedOutStore.class, new RuntimeHandshakeTrustedOutStore(),
                HandshakeTrustedInStore.class, new RuntimeHandshakeTrustedInStore(),
                HandshakeSessionOutStore.class, new RuntimeHandshakeSessionOutStore(),
                HandshakeSessionInStore.class, new RuntimeHandshakeSessionInStore()
        );
        singleValueStores = Map.of(
                AppConfigStore.class, new ImmutableAppConfigStore(() -> {
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
                }),
                DaemonSecretsStore.class, new ImmutableDaemonSecretsStore(() -> {
                    String daemonToken = environment.getRequiredProperty("dropfile.daemon.token");
                    log.info("Provided daemon token: {}", daemonToken);
                    return new DaemonSecrets(daemonToken);
                })
        );
    }

    @Override
    public <T extends KeyValueStore> T requiredStore(Class<T> clazz) {
        checkInitialized();
        KeyValueStore entry = keyValueStores.get(clazz);
        if (entry == null) {
            throw new RuntimeException("No store found for " + clazz.getName());
        }
        return (T) entry;
    }

    @Override
    public <T extends SingleValueStore> T requiredSingleStore(Class<T> clazz) {
        checkInitialized();
        SingleValueStore entry = singleValueStores.get(clazz);
        if (entry == null) {
            throw new RuntimeException("No store found for " + clazz.getName());
        }
        return (T) entry;
    }

    @Override
    public void cacheReset() {
        throw new UnsupportedOperationException("Dev configuration store cannot cache reset");
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
        Path downloadDirectoryPath = Paths.get(System.getProperty("user.home"), "/DropfileDownloads");
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
    public AppConfigStore getUninitializedAppConfigStore() {
        return (AppConfigStore) singleValueStores.get(AppConfigStore.class);
    }
}
