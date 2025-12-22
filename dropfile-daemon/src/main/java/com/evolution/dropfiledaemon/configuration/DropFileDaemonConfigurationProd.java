package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.configuration.app.DropFileAppConfigStore;
import com.evolution.dropfile.configuration.app.DropFileAppConfigStoreInitializationProcedure;
import com.evolution.dropfile.configuration.app.JsonFileDropFileAppConfigStore;
import com.evolution.dropfile.configuration.keys.DropFileKeysConfigStore;
import com.evolution.dropfile.configuration.keys.DropFileKeysConfigStoreInitializationProcedure;
import com.evolution.dropfile.configuration.keys.JsonFileDropFileKeysConfigStore;
import com.evolution.dropfile.configuration.secret.DropFileSecretsConfigStore;
import com.evolution.dropfile.configuration.secret.DropFileSecretsConfigStoreInitializationProcedure;
import com.evolution.dropfile.configuration.secret.JsonFileDropFileSecretsConfigStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("prod")
@Configuration
public class DropFileDaemonConfigurationProd {

    @Bean
    public DropFileAppConfigStoreInitializationProcedure appConfigStoreInitializationProcedure() {
        return new DropFileAppConfigStoreInitializationProcedure();
    }

    @Bean
    public DropFileSecretsConfigStoreInitializationProcedure secretsConfigStoreInitializationProcedure() {
        return new DropFileSecretsConfigStoreInitializationProcedure();
    }

    @Bean
    public DropFileKeysConfigStoreInitializationProcedure keysConfigStoreInitializationProcedure() {
        return new DropFileKeysConfigStoreInitializationProcedure();
    }

    @Bean
    public DropFileAppConfigStore appConfigStore(ObjectMapper objectMapper,
                                                 DropFileAppConfigStoreInitializationProcedure initializationProcedure) {
        DropFileAppConfigStore store = new JsonFileDropFileAppConfigStore(objectMapper);
        initializationProcedure.init(store);
        return store;
    }

    @Bean
    public DropFileSecretsConfigStore secretsConfigStore(ObjectMapper objectMapper,
                                                         DropFileSecretsConfigStoreInitializationProcedure initializationProcedure) {
        DropFileSecretsConfigStore store = new JsonFileDropFileSecretsConfigStore(objectMapper);
        initializationProcedure.init(store);
        return store;
    }

    @Bean
    public DropFileKeysConfigStore keysConfigStore(ObjectMapper objectMapper,
                                                   DropFileKeysConfigStoreInitializationProcedure initializationProcedure) {
        DropFileKeysConfigStore store = new JsonFileDropFileKeysConfigStore(objectMapper);
        initializationProcedure.init(store);
        return store;
    }
}