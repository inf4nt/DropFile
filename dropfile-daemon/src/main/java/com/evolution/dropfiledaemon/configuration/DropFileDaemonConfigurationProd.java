package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfile.store.access.RuntimeAccessKeyStore;
import com.evolution.dropfile.store.app.AppConfigStore;
import com.evolution.dropfile.store.app.AppConfigStoreInitializationProcedure;
import com.evolution.dropfile.store.app.JsonFileAppConfigStore;
import com.evolution.dropfile.store.download.DownloadFileEntryStore;
import com.evolution.dropfile.store.download.JsonFileDownloadFileEntryStore;
import com.evolution.dropfile.store.keys.KeysConfigStore;
import com.evolution.dropfile.store.keys.KeysConfigStoreInitializationProcedure;
import com.evolution.dropfile.store.keys.RuntimeKeysConfigStore;
import com.evolution.dropfile.store.secret.JsonFileSecretsConfigStore;
import com.evolution.dropfile.store.secret.SecretsConfigStore;
import com.evolution.dropfile.store.secret.SecretsConfigStoreInitializationProcedure;
import com.evolution.dropfile.store.store.json.DefaultJsonFileKeyValueStoreInitializationProcedure;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("prod")
@Configuration
public class DropFileDaemonConfigurationProd {

    @Bean
    public AppConfigStoreInitializationProcedure appConfigStoreInitializationProcedure() {
        return new AppConfigStoreInitializationProcedure();
    }

    @Bean
    public SecretsConfigStoreInitializationProcedure secretsConfigStoreInitializationProcedure() {
        return new SecretsConfigStoreInitializationProcedure();
    }

    @Bean
    public KeysConfigStoreInitializationProcedure keysConfigStoreInitializationProcedure() {
        return new KeysConfigStoreInitializationProcedure();
    }

    @Bean
    public AppConfigStore appConfigStore(ObjectMapper objectMapper,
                                         AppConfigStoreInitializationProcedure initializationProcedure) {
        AppConfigStore store = new JsonFileAppConfigStore(objectMapper);
        initializationProcedure.init(store);
        return store;
    }

    @Bean
    public SecretsConfigStore secretsConfigStore(ObjectMapper objectMapper,
                                                 SecretsConfigStoreInitializationProcedure initializationProcedure) {
        SecretsConfigStore store = new JsonFileSecretsConfigStore(objectMapper);
        initializationProcedure.init(store);
        return store;
    }

    @Bean
    public KeysConfigStore keysConfigStore(ObjectMapper objectMapper,
                                           KeysConfigStoreInitializationProcedure initializationProcedure) {
//        KeysConfigStore store = new JsonFileKeysConfigStore(objectMapper);
        KeysConfigStore store = new RuntimeKeysConfigStore();
        initializationProcedure.init(store);
        return store;
    }

    @Bean
    public AccessKeyStore accessKeyStore(ObjectMapper objectMapper,
                                         DefaultJsonFileKeyValueStoreInitializationProcedure initializationProcedure) {
//        AccessKeyStore accessKeyStore = new JsonFileAccessKeyStore(objectMapper);
        AccessKeyStore accessKeyStore = new RuntimeAccessKeyStore();
        initializationProcedure.init(accessKeyStore);
        return accessKeyStore;
    }

    @Bean
    public DownloadFileEntryStore downloadFileEntryStore(ObjectMapper objectMapper,
                                                         DefaultJsonFileKeyValueStoreInitializationProcedure initializationProcedure) {
        DownloadFileEntryStore fileEntryStore = new JsonFileDownloadFileEntryStore(objectMapper);
        initializationProcedure.init(fileEntryStore);
        return fileEntryStore;
    }
}