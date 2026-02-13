package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfile.store.access.RuntimeAccessKeyStore;
import com.evolution.dropfile.store.app.AppConfigStore;
import com.evolution.dropfile.store.app.AppConfigStoreInitializationProcedure;
import com.evolution.dropfile.store.app.JsonFileAppConfigStore;
import com.evolution.dropfile.store.download.FileDownloadEntryStore;
import com.evolution.dropfile.store.download.JsonFileFileDownloadEntryStore;
import com.evolution.dropfile.store.keys.KeysConfigStore;
import com.evolution.dropfile.store.keys.KeysConfigStoreInitializationProcedure;
import com.evolution.dropfile.store.keys.RuntimeKeysConfigStore;
import com.evolution.dropfile.store.secret.JsonFileSecretsConfigStore;
import com.evolution.dropfile.store.secret.SecretsConfigStore;
import com.evolution.dropfile.store.secret.SecretsConfigStoreInitializationProcedure;
import com.evolution.dropfile.store.share.RuntimeShareFileEntryStore;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
import com.evolution.dropfile.store.store.json.DefaultJsonFileKeyValueStoreInitializationProcedure;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStore;
import com.evolution.dropfiledaemon.handshake.store.runtime.RuntimeTrustedInKeyValueStore;
import com.evolution.dropfiledaemon.handshake.store.runtime.RuntimeTrustedOutKeyValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("prod")
@Configuration
class ApplicationConfigStoreProd
        implements ApplicationConfigStore, AppConfigStoreUninitialized, ApplicationListener<ApplicationReadyEvent> {

    private final ApplicationEventPublisher eventPublisher;

    private final DefaultJsonFileKeyValueStoreInitializationProcedure defaultJsonFileKeyValueStoreInitializationProcedure;

    private final AppConfigStore appConfigStore;

    private final AppConfigStoreInitializationProcedure appConfigStoreInitializationProcedure;

    private final SecretsConfigStore secretsConfigStore;

    private final SecretsConfigStoreInitializationProcedure secretsConfigStoreInitializationProcedure;

    private final KeysConfigStore keysConfigStore;

    private final KeysConfigStoreInitializationProcedure keysConfigStoreInitializationProcedure;

    private final AccessKeyStore accessKeyStore;

    private final FileDownloadEntryStore fileDownloadEntryStore;

    private final ShareFileEntryStore shareFileEntryStore;

    private final HandshakeStore handshakeStore;

    private boolean initialized = false;

    @Autowired
    public ApplicationConfigStoreProd(ApplicationEventPublisher eventPublisher, ObjectMapper objectMapper) {
        this.eventPublisher = eventPublisher;

        defaultJsonFileKeyValueStoreInitializationProcedure = new DefaultJsonFileKeyValueStoreInitializationProcedure();

        appConfigStore = new JsonFileAppConfigStore(objectMapper);
        appConfigStoreInitializationProcedure = new AppConfigStoreInitializationProcedure();

        secretsConfigStore = new JsonFileSecretsConfigStore(objectMapper);
        secretsConfigStoreInitializationProcedure = new SecretsConfigStoreInitializationProcedure();

        keysConfigStore = new RuntimeKeysConfigStore();
        keysConfigStoreInitializationProcedure = new KeysConfigStoreInitializationProcedure();

        accessKeyStore = new RuntimeAccessKeyStore();
        fileDownloadEntryStore = new JsonFileFileDownloadEntryStore(objectMapper);
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

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        appConfigStoreInitializationProcedure.init(appConfigStore);
        secretsConfigStoreInitializationProcedure.init(secretsConfigStore);
        keysConfigStoreInitializationProcedure.init(keysConfigStore);

        defaultJsonFileKeyValueStoreInitializationProcedure.init(accessKeyStore);
        defaultJsonFileKeyValueStoreInitializationProcedure.init(fileDownloadEntryStore);

        initialized = true;

        eventPublisher.publishEvent(new ApplicationConfigStoreInitialized());
    }

    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Application has not been initialized yet");
        }
    }
}
