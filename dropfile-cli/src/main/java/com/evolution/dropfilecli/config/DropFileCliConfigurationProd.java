package com.evolution.dropfilecli.config;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.secret.CacheableCryptoDaemonSecretsStore;
import com.evolution.dropfile.store.secret.DaemonSecretsStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.nio.file.Paths;

@Profile("prod")
@Configuration
public class DropFileCliConfigurationProd {

    @Bean
    public FileHelper fileHelper(CliApplicationProperties applicationProperties) {
        return new FileHelper(applicationProperties.fileOperationsBufferSize);
    }

    @Bean
    public DaemonSecretsStore secretsConfigStore(FileHelper fileHelper,
                                                 ObjectMapper objectMapper,
                                                 CryptoTunnel cryptoTunnel,
                                                 CliApplicationProperties cliApplicationProperties) {
        return new CacheableCryptoDaemonSecretsStore(
                fileHelper,
                objectMapper,
                cryptoTunnel,
                Paths.get(cliApplicationProperties.configDirectory)
        );
    }
}
