package com.evolution.dropfilecli.config;

import com.evolution.dropfile.common.SystemInfoProvider;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.common.crypto.CryptoTunnelChaCha20Poly1305;
import com.evolution.dropfile.common.io.FileHelper;
import com.evolution.dropfile.store.framework.KeyValueStore;
import com.evolution.dropfile.store.framework.file.*;
import com.evolution.dropfile.store.secret.DaemonSecret;
import com.evolution.dropfile.store.secret.DaemonSecretStore;
import com.evolution.dropfile.store.secret.DaemonSecretStoreImpl;
import com.evolution.dropfile.store.seed.InstallationSeedBootstrapStore;
import com.evolution.dropfile.store.seed.InstallationSeedBootstrapStoreCacheable;
import com.evolution.dropfilecli.util.DateUtils;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.UUID;

@Configuration
public class DropFileCliConfiguration {

    @Bean
    public SystemInfoProvider systemInfoProvider() {
        return new SystemInfoProvider();
    }

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder().build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setDateFormat(new SimpleDateFormat());
        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(Instant.class, new JsonSerializer<>() {
            @Override
            public void serialize(Instant value,
                                  JsonGenerator gen,
                                  SerializerProvider serializers)
                    throws IOException {

                gen.writeString(DateUtils.FORMATTER.format(value));
            }
        });
        objectMapper.registerModule(module);
        return objectMapper;
    }

    @Bean
    public CryptoTunnel cryptoTunnel() {
        return new CryptoTunnelChaCha20Poly1305();
    }

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
        KeyValueStore<DaemonSecret> store = new FileKeyValueStore<>(
                daemonSecretsFileProvider, fileOperations, serdeOperations
        );
        return new DaemonSecretStoreImpl(store);
    }

    @Bean
    public InstallationSeedBootstrapStore installationSeedBootstrapStore(FileProvider installationSeedFileProvider,
                                                                         FileOperations fileOperations,
                                                                         ObjectMapper objectMapper) {
        SerdeOperations<UUID> serdeOperations = new JsonSerdeOperations<>(
                objectMapper,
                UUID.class
        );
        CacheableFileKeyValueStore<UUID> store = new CacheableFileKeyValueStore<>(
                installationSeedFileProvider,
                fileOperations,
                serdeOperations
        );
        return new InstallationSeedBootstrapStoreCacheable(store);
    }
}
