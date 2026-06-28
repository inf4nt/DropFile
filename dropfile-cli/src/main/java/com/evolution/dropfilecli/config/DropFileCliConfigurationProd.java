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
    public FileProvider daemonSecretFileProvider(DirectoryProvider applicationConfigDirectoryProvider) {
        return new FileProviderImpl(applicationConfigDirectoryProvider, Paths.get(".daemon.bin"));
    }

    @Bean
    public DaemonSecretsStore daemonSecretsStore(FileProvider daemonSecretFileProvider,
                                                 CryptoFileOperations fileOperations,
                                                 ObjectMapper objectMapper) {
        SerdeOperations<DaemonSecrets> serdeOperations = new JsonSerdeOperations<>(
                objectMapper,
                DaemonSecrets.class
        );
        return new DaemonSecretsStoreImpl(
                new CacheFileKeyValueStore<>(
                        daemonSecretFileProvider, fileOperations, serdeOperations
                )
        );
    }

    @Bean
    public FileProvider installationSeedFileProvider(DirectoryProvider applicationConfigDirectoryProvider) {
        return new FileProviderImpl(applicationConfigDirectoryProvider, Paths.get(".installation.bin"));
    }

    @Bean
    public InstallationSeedBootstrapStore installationSeedBootstrapStore(FileProvider installationSeedFileProvider,
                                                                         FileOperations fileOperations,
                                                                         ObjectMapper objectMapper) {
        SerdeOperations<UUID> serdeOperations = new JsonSerdeOperations<>(
                objectMapper,
                UUID.class
        );
        return new InstallationSeedBootstrapStoreImpl(
                new CacheFileKeyValueStore<>(
                        installationSeedFileProvider,
                        fileOperations,
                        serdeOperations
                )
        );
    }
}
