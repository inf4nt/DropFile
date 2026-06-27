package com.evolution.dropfilecli.config;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.framework.file.*;
import com.evolution.dropfile.store.secret.DaemonSecrets;
import com.evolution.dropfile.store.secret.DaemonSecretsStore;
import com.evolution.dropfile.store.secret.DaemonSecretsStoreImpl;
import com.evolution.dropfile.store.seed.InstallationSeedBootstrapStore;
import com.evolution.dropfile.store.seed.InstallationSeedBootstrapStoreImpl;
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
    public DaemonSecretsStore daemonSecretsStore(CryptoFileOperations fileOperations,
                                                 ObjectMapper objectMapper,
                                                 CliApplicationProperties cliApplicationProperties) {
        FileProvider fileProvider = new FileProviderImpl(
                Paths.get(cliApplicationProperties.configDirectory),
                ".daemon.bin"
        );
        SerdeOperations<DaemonSecrets> serdeOperations = new JsonSerdeOperations<>(
                objectMapper,
                DaemonSecrets.class
        );
        return new DaemonSecretsStoreImpl(
                new CacheFileKeyValueStore<>(
                        fileProvider, fileOperations, serdeOperations
                )
        );
    }

    @Bean
    public InstallationSeedBootstrapStore installationSeedBootstrapStore(CliApplicationProperties applicationProperties,
                                                                         FileOperations fileOperations,
                                                                         ObjectMapper objectMapper) {
        FileProvider fileProvider = new FileProviderImpl(
                Paths.get(applicationProperties.configDirectory),
                ".installation.bin"
        );
        SerdeOperations<UUID> serdeOperations = new JsonSerdeOperations<>(
                objectMapper,
                UUID.class
        );
        return new InstallationSeedBootstrapStoreImpl(
                new CacheFileKeyValueStore<>(
                        fileProvider,
                        fileOperations,
                        serdeOperations
                )
        );
    }
}
