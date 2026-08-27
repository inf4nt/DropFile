package com.evolution.dropfilecli.config;

import com.evolution.dropfile.common.io.FileHelper;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.framework.file.*;
import com.evolution.dropfile.store.secret.DaemonSecret;
import com.evolution.dropfile.store.secret.DaemonSecretStore;
import com.evolution.dropfile.store.secret.DaemonSecretStoreImpl;
import com.evolution.dropfile.store.seed.InstallationSeedBootstrapStore;
import com.evolution.dropfile.store.seed.InstallationSeedBootstrapStoreCacheable;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.nio.file.Paths;
import java.util.UUID;

@Profile("prod")
@Configuration
public class DropFileCliConfigurationProd {

    @Bean
    public FileHelper fileHelper() {
        return new FileHelper();
    }

    @Primary
    @Bean
    public FileSystemOperations fileSystemOperations(FileHelper fileHelper) {
        return new FileSystemOperations(fileHelper);
    }

    @Bean
    public CryptoFileOperations cryptoFileOperations(FileOperations fileOperations,
                                                     CryptoTunnel cryptoTunnel,
                                                     InstallationSeedBootstrapStore installationSeedBootstrapStore) {
        return new CryptoFileOperations(
                fileOperations,
                cryptoTunnel,
                installationSeedBootstrapStore
        );
    }

    @Bean
    public FileProvider daemonSecretsFileProvider(CliApplicationProperties cliApplicationProperties) {
        return new FileProviderImpl(
                new DirectoryProviderImpl(cliApplicationProperties.daemonSecretsDirectory),
                Paths.get(".daemon.bin")
        );
    }

    @Bean
    public FileProvider installationSeedFileProvider(CliApplicationProperties cliApplicationProperties) {
        return new FileProviderImpl(
                new DirectoryProviderImpl(cliApplicationProperties.daemonInstallationSeedDirectory),
                Paths.get(".installation.json")
        );
    }

    @Bean
    public DaemonSecretStore daemonSecretStore(FileProvider daemonSecretsFileProvider,
                                                CryptoFileOperations fileOperations,
                                                ObjectMapper objectMapper) {
        SerdeOperations<DaemonSecret> serdeOperations = new JsonSerdeOperations<>(
                objectMapper,
                DaemonSecret.class
        );
        return new DaemonSecretStoreImpl(
                new FileKeyValueStore<>(
                        daemonSecretsFileProvider, fileOperations, serdeOperations
                )
        );
    }

    @Bean
    public InstallationSeedBootstrapStore installationSeedBootstrapStore(FileProvider installationSeedFileProvider,
                                                                         FileOperations fileOperations,
                                                                         ObjectMapper objectMapper) {
        SerdeOperations<UUID> serdeOperations = new JsonSerdeOperations<>(
                objectMapper,
                UUID.class
        );
        return new InstallationSeedBootstrapStoreCacheable(
                new CacheableFileKeyValueStore<>(
                        installationSeedFileProvider,
                        fileOperations,
                        serdeOperations
                )
        );
    }
}
