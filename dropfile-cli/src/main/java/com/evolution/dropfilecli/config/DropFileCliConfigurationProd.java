package com.evolution.dropfilecli.config;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.framework.file.ApplicationFingerprintSupplier;
import com.evolution.dropfile.store.framework.file.ApplicationFingerprintSupplierImpl;
import com.evolution.dropfile.store.framework.file.FileProvider;
import com.evolution.dropfile.store.framework.file.FileProviderImpl;
import com.evolution.dropfile.store.secret.CryptoCacheDaemonSecretsStore;
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
    public FileHelper fileHelper() {
        return new FileHelper();
    }

    @Bean
    public DaemonSecretsStore secretsConfigStore(FileHelper fileHelper,
                                                 ObjectMapper objectMapper,
                                                 CryptoTunnel cryptoTunnel,
                                                 ApplicationFingerprintSupplier applicationFingerprintSupplier,
                                                 CliApplicationProperties cliApplicationProperties) {
        return new CryptoCacheDaemonSecretsStore(
                fileHelper,
                objectMapper,
                cryptoTunnel,
                applicationFingerprintSupplier,
                Paths.get(cliApplicationProperties.configDirectory)
        );
    }

    @Bean
    public ApplicationFingerprintSupplier applicationFingerprintSupplier(CliApplicationProperties cliApplicationProperties) {
        FileProvider fileProvider = new FileProviderImpl(
                Paths.get(cliApplicationProperties.configDirectory),
                ".fingerprint.bin"
        );
        return new ApplicationFingerprintSupplierImpl(fileProvider);
    }
}
