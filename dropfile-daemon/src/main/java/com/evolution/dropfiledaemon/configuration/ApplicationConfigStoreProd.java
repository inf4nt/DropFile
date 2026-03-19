package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfile.store.access.RuntimeAccessKeyStore;
import com.evolution.dropfile.store.download.FileDownloadEntryStore;
import com.evolution.dropfile.store.download.JsonFileFileDownloadEntryStore;
import com.evolution.dropfile.store.framework.Cacheable;
import com.evolution.dropfile.store.framework.KeyValueStore;
import com.evolution.dropfile.store.framework.KeyValueStoreInitializationProcedure;
import com.evolution.dropfile.store.framework.single.SingleValueStore;
import com.evolution.dropfile.store.framework.single.StoreInitializationProcedure;
import com.evolution.dropfile.store.secret.CryptoDaemonSecretsStore;
import com.evolution.dropfile.store.secret.DaemonSecretsStore;
import com.evolution.dropfile.store.share.JsonFileShareFileEntryStore;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
import com.evolution.dropfiledaemon.configuration.middleware.DaemonSecretsStoreInitializationProcedure;
import com.evolution.dropfiledaemon.configuration.middleware.FileDownloadEntryStoreKeyValueStoreInitializationProcedure;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionInStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionOutStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.handshake.store.crypto.CryptoHandshakeTrustedInStore;
import com.evolution.dropfiledaemon.handshake.store.crypto.CryptoHandshakeTrustedOutStore;
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

import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Map;

@Slf4j
@Profile("prod")
@Configuration
class ApplicationConfigStoreProd
        implements ApplicationConfigStore, ApplicationListener<ApplicationReadyEvent> {

    private boolean initialized = false;

    private final ApplicationEventPublisher eventPublisher;

    private final Map<Class, Map.Entry<? extends KeyValueStore, ? extends KeyValueStoreInitializationProcedure>> keyValueStores;

    private final Map<Class, Map.Entry<SingleValueStore, StoreInitializationProcedure>> singleValueStores;

    @Autowired
    public ApplicationConfigStoreProd(ApplicationEventPublisher eventPublisher,
                                      FileHelper fileHelper,
                                      ObjectMapper objectMapper,
                                      CryptoTunnel cryptoTunnel,
                                      DaemonApplicationProperties daemonApplicationProperties) {
        this.eventPublisher = eventPublisher;
        String configDirectory = daemonApplicationProperties.configDirectory;

        keyValueStores = Map.of(
                FileDownloadEntryStore.class, new AbstractMap.SimpleEntry<>(
                        new JsonFileFileDownloadEntryStore(fileHelper, objectMapper, Paths.get(configDirectory)),
                        new FileDownloadEntryStoreKeyValueStoreInitializationProcedure()
                ),
                AccessKeyStore.class, new AbstractMap.SimpleEntry<>(
                        new RuntimeAccessKeyStore(),
                        null
                ),
                ShareFileEntryStore.class, new AbstractMap.SimpleEntry<>(
                        new JsonFileShareFileEntryStore(fileHelper, objectMapper, Paths.get(configDirectory)),
                        null
                ),
                HandshakeTrustedOutStore.class, new AbstractMap.SimpleEntry<>(
                        new CryptoHandshakeTrustedOutStore(fileHelper, objectMapper, cryptoTunnel, Paths.get(configDirectory)),
                        null
                ),
                HandshakeTrustedInStore.class, new AbstractMap.SimpleEntry<>(
                        new CryptoHandshakeTrustedInStore(fileHelper, objectMapper, cryptoTunnel, Paths.get(configDirectory)),
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
                DaemonSecretsStore.class, new AbstractMap.SimpleEntry<>(
                        new CryptoDaemonSecretsStore(fileHelper, objectMapper, cryptoTunnel, Paths.get(configDirectory)),
                        new DaemonSecretsStoreInitializationProcedure()
                )
        );
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
            KeyValueStore store = value.getKey();
            log.info("Store initialization {}", store.getClass().getName());
            store.init();

            KeyValueStoreInitializationProcedure initializationProcedure = value.getValue();
            if (initializationProcedure != null) {
                initializationProcedure.init(store);
            }
        }
        for (Map.Entry<SingleValueStore, StoreInitializationProcedure> value : singleValueStores.values()) {
            SingleValueStore store = value.getKey();
            log.info("Store initialization {}", store.getClass().getName());
            store.init();

            StoreInitializationProcedure initializationProcedure = value.getValue();
            initializationProcedure.init(store);
        }

        initialized = true;

        eventPublisher.publishEvent(new ApplicationConfigStoreInitialized());
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

    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Application has not been initialized yet");
        }
    }
}
