package com.evolution.dropfilecli.config;

import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.app.cli.CliAppConfigStore;
import com.evolution.dropfile.store.app.cli.CliAppConfigStoreInitializationProcedure;
import com.evolution.dropfile.store.app.cli.JsonFileCliAppConfigStore;
import com.evolution.dropfile.store.secret.CryptoDaemonSecretsStore;
import com.evolution.dropfile.store.secret.DaemonSecretsStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("prod")
@Configuration
public class DropFileCliConfigurationProd {

    @Bean
    public CliAppConfigStoreInitializationProcedure cliAppConfigStoreInitializationProcedure() {
        return new CliAppConfigStoreInitializationProcedure();
    }

    @Bean
    public CliAppConfigStore cliAppConfigStore(ObjectMapper objectMapper,
                                               CliAppConfigStoreInitializationProcedure initializationProcedure) {
        CliAppConfigStore store = new JsonFileCliAppConfigStore(objectMapper);
        initializationProcedure.init(store);
        return store;
    }

    @Bean
    public DaemonSecretsStore secretsConfigStore(ObjectMapper objectMapper,
                                                 CryptoTunnel cryptoTunnel) {
        return new CryptoDaemonSecretsStore(objectMapper, cryptoTunnel);
    }
}
