package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfile.store.access.RuntimeAccessKeyStore;
import com.evolution.dropfile.store.app.AppConfigStore;
import com.evolution.dropfile.store.app.AppConfigStoreInitializationProcedure;
import com.evolution.dropfile.store.app.CacheableJsonFileAppConfigStore;
import com.evolution.dropfile.store.download.CacheableJsonFileFileDownloadEntryStore;
import com.evolution.dropfile.store.download.FileDownloadEntryStore;
import com.evolution.dropfile.store.framework.Cacheable;
import com.evolution.dropfile.store.framework.DefaultKeyValueStoreInitializationProcedure;
import com.evolution.dropfile.store.framework.KeyValueStore;
import com.evolution.dropfile.store.framework.KeyValueStoreInitializationProcedure;
import com.evolution.dropfile.store.framework.single.SingleValueStore;
import com.evolution.dropfile.store.framework.single.StoreInitializationProcedure;
import com.evolution.dropfile.store.secret.CacheableCryptoDaemonSecretsStore;
import com.evolution.dropfile.store.secret.DaemonSecretsStore;
import com.evolution.dropfile.store.share.RuntimeShareFileEntryStore;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
import com.evolution.dropfiledaemon.configuration.middleware.DaemonSecretsStoreInitializationProcedure;
import com.evolution.dropfiledaemon.configuration.middleware.FileDownloadEntryStoreKeyValueStoreInitializationProcedure;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionInStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionOutStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.handshake.store.crypto.CacheableCryptoHandshakeTrustedInStore;
import com.evolution.dropfiledaemon.handshake.store.crypto.CacheableCryptoHandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.handshake.store.runtime.RuntimeHandshakeSessionInStore;
import com.evolution.dropfiledaemon.handshake.store.runtime.RuntimeHandshakeSessionOutStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.AbstractMap;
import java.util.Map;

@Slf4j
@Profile("prod")
@Configuration
class ApplicationConfigStoreProd
        implements ApplicationConfigStore, AppConfigStoreUninitialized, ApplicationListener<ApplicationReadyEvent> {

    private boolean initialized = false;

    private final ApplicationEventPublisher eventPublisher;

    private final Map<Class, Map.Entry<? extends KeyValueStore, ? extends KeyValueStoreInitializationProcedure>> keyValueStores;

    private final Map<Class, Map.Entry<SingleValueStore, StoreInitializationProcedure>> singleValueStores;

    private final KeyValueStoreInitializationProcedure defaultKeyValueStoreInitializationProcedure = new DefaultKeyValueStoreInitializationProcedure();

    @Autowired
    public ApplicationConfigStoreProd(ApplicationEventPublisher eventPublisher,
                                      ObjectMapper objectMapper,
                                      CryptoTunnel cryptoTunnel) {
        this.eventPublisher = eventPublisher;

        keyValueStores = Map.of(
                FileDownloadEntryStore.class, new AbstractMap.SimpleEntry<>(
                        new CacheableJsonFileFileDownloadEntryStore(objectMapper),
                        new FileDownloadEntryStoreKeyValueStoreInitializationProcedure()
                ),
                AccessKeyStore.class, new AbstractMap.SimpleEntry<>(
                        new RuntimeAccessKeyStore(),
                        null
                ),
                ShareFileEntryStore.class, new AbstractMap.SimpleEntry<>(
                        new RuntimeShareFileEntryStore(),
                        null
                ),
                HandshakeTrustedOutStore.class, new AbstractMap.SimpleEntry<>(
                        new CacheableCryptoHandshakeTrustedOutStore(objectMapper, cryptoTunnel),
                        null
                ),
                HandshakeTrustedInStore.class, new AbstractMap.SimpleEntry<>(
                        new CacheableCryptoHandshakeTrustedInStore(objectMapper, cryptoTunnel),
                        null
                ),
                HandshakeSessionOutStore.class, new AbstractMap.SimpleEntry<>(
                        new RuntimeHandshakeSessionOutStore(),
                        null
                ),
                HandshakeSessionInStore.class, new AbstractMap.SimpleEntry<>(
                        new RuntimeHandshakeSessionInStore(),
                        null
                )
        );
        singleValueStores = Map.of(
                AppConfigStore.class, new AbstractMap.SimpleEntry<>(
                        new CacheableJsonFileAppConfigStore(objectMapper),
                        new AppConfigStoreInitializationProcedure()
                ),
                DaemonSecretsStore.class, new AbstractMap.SimpleEntry<>(
                        new CacheableCryptoDaemonSecretsStore(objectMapper, cryptoTunnel),
                        new DaemonSecretsStoreInitializationProcedure()
                )
        );
    }

    @Override
    public <T extends KeyValueStore> T requiredStore(Class<T> clazz) {
        checkInitialized();
        Map.Entry<? extends KeyValueStore, ? extends KeyValueStoreInitializationProcedure> entry = keyValueStores.get(clazz);
        if (entry == null) {
            throw new RuntimeException("No store found for " + clazz.getName());
        }
        return (T) entry.getKey();
    }

    @Override
    public <T extends SingleValueStore> T requiredSingleStore(Class<T> clazz) {
        checkInitialized();
        Map.Entry<SingleValueStore, StoreInitializationProcedure> entry = singleValueStores.get(clazz);
        if (entry == null) {
            throw new RuntimeException("No store found for " + clazz.getName());
        }
        return (T) entry.getKey();
    }

    @Override
    public AppConfigStore getUninitializedAppConfigStore() {
        return (AppConfigStore) singleValueStores.get(AppConfigStore.class).getKey();
    }

    @Override
    public void cacheReset() {
        checkInitialized();
        for (Map.Entry<? extends KeyValueStore, ? extends KeyValueStoreInitializationProcedure> value : keyValueStores.values()) {
            KeyValueStore keyValueStore = value.getKey();
            if (keyValueStore instanceof Cacheable cacheable) {
                log.info("Cache reset {}", keyValueStore.getClass().getName());
                cacheable.reset();
            }
        }
        for (Map.Entry<SingleValueStore, StoreInitializationProcedure> value : singleValueStores.values()) {
            SingleValueStore singleValueStore = value.getKey();
            if (singleValueStore instanceof Cacheable cacheable) {
                log.info("Cache reset {}", singleValueStore.getClass().getName());
                cacheable.reset();
            }
        }
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        for (Map.Entry<? extends KeyValueStore, ? extends KeyValueStoreInitializationProcedure> value : keyValueStores.values()) {
            KeyValueStore keyValueStore = value.getKey();
            KeyValueStoreInitializationProcedure initializationProcedure = value.getValue();
            if (initializationProcedure != null) {
                initializationProcedure.init(keyValueStore);
            } else {
                defaultKeyValueStoreInitializationProcedure.init(keyValueStore);
            }
        }
        for (Map.Entry<SingleValueStore, StoreInitializationProcedure> value : singleValueStores.values()) {
            SingleValueStore singleValueStore = value.getKey();
            StoreInitializationProcedure initializationProcedure = value.getValue();
            initializationProcedure.init(singleValueStore);
        }

        initialized = true;

        eventPublisher.publishEvent(new ApplicationConfigStoreInitialized());
    }

    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Application has not been initialized yet");
        }
    }
}
