package com.evolution.dropfilecli.config;

import com.evolution.dropfile.configuration.app.*;
import com.evolution.dropfile.configuration.secret.*;
import com.evolution.dropfile.configuration.store.json.JsonFileKeyValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("prod")
@Configuration
public class DropFileCliConfigurationProd {

    @Bean
    public DropFileAppConfigStoreInitializationProcedure appConfigStoreInitializationProcedure() {
        return new DefaultDropFileAppConfigStoreInitializationProcedure();
    }

    @Bean
    public DropFileSecretsConfigStoreInitializationProcedure secretsConfigStoreInitializationProcedure() {
        return new DefaultDropFileSecretsConfigStoreInitializationProcedure();
    }

    @Bean
    public DropFileAppConfigStore appConfigStore(ObjectMapper objectMapper,
                                                 DropFileAppConfigStoreInitializationProcedure initializationProcedure) {
        DefaultDropFileAppConfigStore store = new DefaultDropFileAppConfigStore(
                new JsonFileKeyValueStore<>(
                        new DefaultDropFileAppConfigStoreFileProvider(),
                        new DropFileAppConfigJsonSerde(objectMapper)
                )
        );
        initializationProcedure.init(store);
        return store;
    }

    @Bean
    public DropFileSecretsConfigStore secretsConfigStore(ObjectMapper objectMapper,
                                                         DropFileSecretsConfigStoreInitializationProcedure initializationProcedure) {
        DefaultDropFileSecretsConfigStore store = new DefaultDropFileSecretsConfigStore(
                new JsonFileKeyValueStore<>(
                        new DefaultDropFileSecretsConfigFileProvider(),
                        new DropFileSecretsConfigJsonSerde(objectMapper)
                )
        );
        initializationProcedure.init(store);
        return store;
    }
}
