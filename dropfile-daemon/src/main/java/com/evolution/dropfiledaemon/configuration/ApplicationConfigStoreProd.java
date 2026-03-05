package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfile.store.access.RuntimeAccessKeyStore;
import com.evolution.dropfile.store.app.AppConfigStore;
import com.evolution.dropfile.store.app.AppConfigStoreInitializationProcedure;
import com.evolution.dropfile.store.app.CacheableJsonFileAppConfigStore;
import com.evolution.dropfile.store.download.CacheableJsonFileFileDownloadEntryStore;
import com.evolution.dropfile.store.download.FileDownloadEntryStore;
import com.evolution.dropfile.store.framework.DefaultKeyValueStoreInitializationProcedure;
import com.evolution.dropfile.store.secret.CacheableCryptoDaemonSecretsStore;
import com.evolution.dropfile.store.secret.DaemonSecretsStore;
import com.evolution.dropfile.store.share.RuntimeShareFileEntryStore;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
import com.evolution.dropfiledaemon.configuration.middleware.DaemonSecretsStoreInitializationProcedure;
import com.evolution.dropfiledaemon.configuration.middleware.FileDownloadEntryStoreKeyValueStoreInitializationProcedure;
import com.evolution.dropfiledaemon.handshake.store.*;
import com.evolution.dropfiledaemon.handshake.store.crypto.CacheableCryptoHandshakeTrustedInStore;
import com.evolution.dropfiledaemon.handshake.store.crypto.CacheableCryptoHandshakeTrustedOutStore;
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

    private final FileDownloadEntryStoreKeyValueStoreInitializationProcedure fileDownloadEntryStoreKeyValueStoreInitializationProcedure;

    private final ShareFileEntryStore shareFileEntryStore;

    private final HandshakeTrustedOutStore handshakeTrustedOutStore;

    private final HandshakeTrustedInStore handshakeTrustedInStore;

    private final HandshakeSessionOutStore handshakeSessionOutStore;

    private final HandshakeSessionInStore handshakeSessionInStore;

    @Autowired
    public ApplicationConfigStoreProd(ApplicationEventPublisher eventPublisher,
                                      ObjectMapper objectMapper,
                                      CryptoTunnel cryptoTunnel) {
        this.eventPublisher = eventPublisher;

        defaultKeyValueStoreInitializationProcedure = new DefaultKeyValueStoreInitializationProcedure();

        appConfigStore = new CacheableJsonFileAppConfigStore(objectMapper);
        appConfigStoreInitializationProcedure = new AppConfigStoreInitializationProcedure();

        daemonSecretsStore = new CacheableCryptoDaemonSecretsStore(objectMapper, cryptoTunnel);
        daemonSecretsStoreInitializationProcedure = new DaemonSecretsStoreInitializationProcedure();

        fileDownloadEntryStore = new CacheableJsonFileFileDownloadEntryStore(objectMapper);
        fileDownloadEntryStoreKeyValueStoreInitializationProcedure = new FileDownloadEntryStoreKeyValueStoreInitializationProcedure();

        accessKeyStore = new RuntimeAccessKeyStore();

        // TODO make shareFileEntryStore as persistence
        shareFileEntryStore = new RuntimeShareFileEntryStore();

        handshakeTrustedOutStore = new CacheableCryptoHandshakeTrustedOutStore(objectMapper, cryptoTunnel);

        handshakeTrustedInStore = new CacheableCryptoHandshakeTrustedInStore(objectMapper, cryptoTunnel);

        handshakeSessionOutStore = new RuntimeHandshakeSessionOutStore();

        handshakeSessionInStore = new RuntimeHandshakeSessionInStore();
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
    public HandshakeTrustedOutStore getHandshakeTrustedOutStore() {
        checkInitialized();
        return handshakeTrustedOutStore;
    }

    @Override
    public HandshakeTrustedInStore getHandshakeTrustedInStore() {
        checkInitialized();
        return handshakeTrustedInStore;
    }

    @Override
    public HandshakeSessionOutStore getHandshakeSessionOutStore() {
        checkInitialized();
        return handshakeSessionOutStore;
    }

    @Override
    public HandshakeSessionInStore getHandshakeSessionInStore() {
        checkInitialized();
        return handshakeSessionInStore;
    }

    @Override
    public void cacheReset() {
        // TODO
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        appConfigStoreInitializationProcedure.init(appConfigStore);
        daemonSecretsStoreInitializationProcedure.init(daemonSecretsStore);
        fileDownloadEntryStoreKeyValueStoreInitializationProcedure.init(fileDownloadEntryStore);

        defaultKeyValueStoreInitializationProcedure.init(accessKeyStore);
        defaultKeyValueStoreInitializationProcedure.init(shareFileEntryStore);

        defaultKeyValueStoreInitializationProcedure.init(handshakeTrustedInStore);
        defaultKeyValueStoreInitializationProcedure.init(handshakeTrustedOutStore);
        defaultKeyValueStoreInitializationProcedure.init(handshakeSessionOutStore);
        defaultKeyValueStoreInitializationProcedure.init(handshakeSessionInStore);

        initialized = true;

        eventPublisher.publishEvent(new ApplicationConfigStoreInitialized());
    }

    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Application has not been initialized yet");
        }
    }
}
