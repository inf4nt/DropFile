package com.evolution.dropfilecli.config;

import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.app.AppConfigStore;
import com.evolution.dropfile.store.app.AppConfigStoreInitializationProcedure;
import com.evolution.dropfile.store.app.JsonFileAppConfigStore;
import com.evolution.dropfile.store.secret.CryptoDaemonSecretsStore;
import com.evolution.dropfile.store.secret.DaemonSecretsStore;
import com.evolution.dropfile.store.secret.DaemonSecretsStoreInitializationProcedure;
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
    public DaemonSecretsStoreInitializationProcedure secretsConfigStoreInitializationProcedure() {
        return new DaemonSecretsStoreInitializationProcedure();
    }

    @Bean
    public AppConfigStore appConfigStore(ObjectMapper objectMapper,
                                         AppConfigStoreInitializationProcedure initializationProcedure) {
        AppConfigStore store = new JsonFileAppConfigStore(objectMapper);
        initializationProcedure.init(store);
        return store;
    }

    @Bean
    public DaemonSecretsStore secretsConfigStore(ObjectMapper objectMapper,
                                                 CryptoTunnel cryptoTunnel) {
        return new CryptoDaemonSecretsStore(objectMapper, cryptoTunnel);
    }
}
