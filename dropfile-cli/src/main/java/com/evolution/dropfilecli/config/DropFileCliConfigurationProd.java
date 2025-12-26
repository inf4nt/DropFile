package com.evolution.dropfilecli.config;

import com.evolution.dropfile.configuration.app.AppConfigStore;
import com.evolution.dropfile.configuration.app.AppConfigStoreInitializationProcedure;
import com.evolution.dropfile.configuration.app.JsonFileAppConfigStore;
import com.evolution.dropfile.configuration.secret.SecretsConfigStore;
import com.evolution.dropfile.configuration.secret.SecretsConfigStoreInitializationProcedure;
import com.evolution.dropfile.configuration.secret.JsonFileSecretsConfigStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("prod")
@Configuration
public class DropFileCliConfigurationProd {

    @Bean
    public AppConfigStoreInitializationProcedure appConfigStoreInitializationProcedure() {
        return new AppConfigStoreInitializationProcedure();
    }

    @Bean
    public SecretsConfigStoreInitializationProcedure secretsConfigStoreInitializationProcedure() {
        return new SecretsConfigStoreInitializationProcedure();
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
}
