package com.evolution.dropfilecli.config;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.framework.file.InstallationSeedProvider;
import com.evolution.dropfile.store.framework.file.FileSystemInstallationSeedProvider;
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
                                                 InstallationSeedProvider installationSeedProvider,
                                                 CliApplicationProperties cliApplicationProperties) {
        return new CryptoCacheDaemonSecretsStore(
                fileHelper,
                objectMapper,
                cryptoTunnel,
                installationSeedProvider,
                Paths.get(cliApplicationProperties.configDirectory)
        );
    }

    @Bean
    public InstallationSeedProvider applicationFingerprintSupplier(CliApplicationProperties cliApplicationProperties) {
        FileProvider fileProvider = new FileProviderImpl(
                Paths.get(cliApplicationProperties.configDirectory),
                ".fingerprint.bin"
        );
        return new FileSystemInstallationSeedProvider(fileProvider);
    }
}
