package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfile.store.access.RuntimeAccessKeyStore;
import com.evolution.dropfile.store.app.AppConfigStore;
import com.evolution.dropfile.store.app.AppConfigStoreInitializationProcedure;
import com.evolution.dropfile.store.app.JsonFileAppConfigStore;
import com.evolution.dropfile.store.download.FileDownloadEntryStore;
import com.evolution.dropfile.store.download.JsonFileFileDownloadEntryStore;
import com.evolution.dropfile.store.secret.CryptoDaemonSecretsStore;
import com.evolution.dropfile.store.secret.DaemonSecretsStore;
import com.evolution.dropfile.store.secret.DaemonSecretsStoreInitializationProcedure;
import com.evolution.dropfile.store.share.RuntimeShareFileEntryStore;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
import com.evolution.dropfile.store.framework.DefaultKeyValueStoreInitializationProcedure;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStore;
import com.evolution.dropfiledaemon.handshake.store.crypto.CryptoHandshakeTrustedInStore;
import com.evolution.dropfiledaemon.handshake.store.crypto.CryptoHandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.handshake.store.runtime.RuntimeHandshakeSessionInStore;
import com.evolution.dropfiledaemon.handshake.store.runtime.RuntimeHandshakeSessionOutStore;
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

    private boolean initialized = false;

    private final ApplicationEventPublisher eventPublisher;

    private final DefaultKeyValueStoreInitializationProcedure defaultKeyValueStoreInitializationProcedure;

    private final AppConfigStore appConfigStore;

    private final AppConfigStoreInitializationProcedure appConfigStoreInitializationProcedure;

    private final DaemonSecretsStore daemonSecretsStore;

    private final DaemonSecretsStoreInitializationProcedure daemonSecretsStoreInitializationProcedure;

    private final AccessKeyStore accessKeyStore;

    private final FileDownloadEntryStore fileDownloadEntryStore;

    private final ShareFileEntryStore shareFileEntryStore;

    private final HandshakeStore handshakeStore;

    @Autowired
    public ApplicationConfigStoreProd(ApplicationEventPublisher eventPublisher,
                                      ObjectMapper objectMapper,
                                      CryptoTunnel cryptoTunnel) {
        this.eventPublisher = eventPublisher;

        defaultKeyValueStoreInitializationProcedure = new DefaultKeyValueStoreInitializationProcedure();

        appConfigStore = new JsonFileAppConfigStore(objectMapper);
        appConfigStoreInitializationProcedure = new AppConfigStoreInitializationProcedure();

        daemonSecretsStore = new CryptoDaemonSecretsStore(objectMapper, cryptoTunnel);
        daemonSecretsStoreInitializationProcedure = new DaemonSecretsStoreInitializationProcedure();

        fileDownloadEntryStore = new JsonFileFileDownloadEntryStore(objectMapper);

        accessKeyStore = new RuntimeAccessKeyStore();

        // TODO make shareFileEntryStore as persistence
        shareFileEntryStore = new RuntimeShareFileEntryStore();

        handshakeStore = new HandshakeStore(
                new CryptoHandshakeTrustedOutStore(objectMapper, cryptoTunnel),
                new CryptoHandshakeTrustedInStore(objectMapper, cryptoTunnel),
                new RuntimeHandshakeSessionOutStore(),
                new RuntimeHandshakeSessionInStore()
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
    public DaemonSecretsStore getSecretsConfigStore() {
        checkInitialized();
        return daemonSecretsStore;
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
        daemonSecretsStoreInitializationProcedure.init(daemonSecretsStore);

        defaultKeyValueStoreInitializationProcedure.init(accessKeyStore);
        defaultKeyValueStoreInitializationProcedure.init(fileDownloadEntryStore);
        defaultKeyValueStoreInitializationProcedure.init(shareFileEntryStore);

        defaultKeyValueStoreInitializationProcedure.init(handshakeStore.trustedInStore());
        defaultKeyValueStoreInitializationProcedure.init(handshakeStore.trustedOutStore());
        defaultKeyValueStoreInitializationProcedure.init(handshakeStore.sessionOutStore());
        defaultKeyValueStoreInitializationProcedure.init(handshakeStore.sessionInStore());

        initialized = true;

        eventPublisher.publishEvent(new ApplicationConfigStoreInitialized());
    }

    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Application has not been initialized yet");
        }
    }
}
