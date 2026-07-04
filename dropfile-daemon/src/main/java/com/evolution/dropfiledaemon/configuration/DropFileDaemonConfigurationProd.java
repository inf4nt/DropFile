package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfile.store.access.RuntimeAccessKeyStore;
import com.evolution.dropfile.store.download.DownloadFileEntry;
import com.evolution.dropfile.store.download.FileDownloadEntryStore;
import com.evolution.dropfile.store.download.FileDownloadEntryStoreImpl;
import com.evolution.dropfile.store.framework.file.*;
import com.evolution.dropfile.store.quickshare.QuickShareEntryStore;
import com.evolution.dropfile.store.quickshare.RuntimeQuickShareEntryStore;
import com.evolution.dropfile.store.secret.DaemonSecrets;
import com.evolution.dropfile.store.secret.DaemonSecretsStore;
import com.evolution.dropfile.store.secret.DaemonSecretsStoreImpl;
import com.evolution.dropfile.store.seed.InstallationSeedBootstrapStore;
import com.evolution.dropfile.store.seed.InstallationSeedBootstrapStoreImpl;
import com.evolution.dropfile.store.share.ShareFileEntry;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
import com.evolution.dropfile.store.share.ShareFileEntryStoreImpl;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionInStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionOutStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.handshake.store.crypto.HandshakeTrustedInStoreImpl;
import com.evolution.dropfiledaemon.handshake.store.crypto.HandshakeTrustedOutStoreImpl;
import com.evolution.dropfiledaemon.handshake.store.runtime.RuntimeHandshakeSessionInStore;
import com.evolution.dropfiledaemon.handshake.store.runtime.RuntimeHandshakeSessionOutStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Profile("prod")
@Configuration
public class DropFileDaemonConfigurationProd {

    @Bean
    public DirectoryProvider daemonSecretsDirectoryProvider(DaemonApplicationProperties applicationProperties) {
        return new DirectoryProviderImpl(applicationProperties.daemonSecretsDirectory);
    }

    @Bean
    public DirectoryProvider daemonInstallationSeedDirectoryProvider(DaemonApplicationProperties applicationProperties) {
        return new DirectoryProviderImpl(applicationProperties.daemonInstallationSeedDirectory);
    }

    @Bean
    public DirectoryProvider daemonConfigDirectoryProvider(DaemonApplicationProperties applicationProperties) {
        return new DirectoryProviderImpl(applicationProperties.daemonConfigDirectory);
    }

    @Bean
    public FileProvider downloadEntriesFileProvider(DirectoryProvider daemonConfigDirectoryProvider) {
        return new FileProviderImpl(daemonConfigDirectoryProvider, Paths.get("download.entries.json"));
    }

    @Bean
    public FileProvider shareEntriesFileProvider(DirectoryProvider daemonConfigDirectoryProvider) {
        return new FileProviderImpl(daemonConfigDirectoryProvider, Paths.get("share.entries.json"));
    }

    @Bean
    public FileProvider trustOutFileProvider(DirectoryProvider daemonConfigDirectoryProvider) {
        return new FileProviderImpl(daemonConfigDirectoryProvider, Paths.get(".trustout.bin"));
    }

    @Bean
    public FileProvider trustInFileProvider(DirectoryProvider daemonConfigDirectoryProvider) {
        return new FileProviderImpl(daemonConfigDirectoryProvider, Paths.get(".trustin.bin"));
    }

    @Bean
    public FileProvider daemonSecretFileProvider(DirectoryProvider daemonSecretsDirectoryProvider) {
        return new FileProviderImpl(daemonSecretsDirectoryProvider, Paths.get(".daemon.bin"));
    }

    @Bean
    public FileProvider installationSeedFileProvider(DirectoryProvider daemonInstallationSeedDirectoryProvider) {
        return new FileProviderImpl(daemonInstallationSeedDirectoryProvider, Paths.get(".installation.bin"));
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
    public FileDownloadEntryStore fileDownloadEntryStore(FileProvider downloadEntriesFileProvider,
                                                         FileOperations fileOperations,
                                                         ObjectMapper objectMapper) {
        SerdeOperations<DownloadFileEntry> serdeOperations = new JsonSerdeOperations<>(
                objectMapper,
                DownloadFileEntry.class
        );
        return new FileDownloadEntryStoreImpl(
                downloadEntriesFileProvider,
                fileOperations,
                serdeOperations
        );
    }

    @Bean
    public AccessKeyStore accessKeyStore() {
        return new RuntimeAccessKeyStore();
    }

    @Bean
    public ShareFileEntryStore shareFileEntryStore(FileProvider shareEntriesFileProvider,
                                                   FileOperations fileOperations,
                                                   ObjectMapper objectMapper) {
        SerdeOperations<ShareFileEntry> serdeOperations = new JsonSerdeOperations<>(
                objectMapper,
                ShareFileEntry.class
        );
        return new ShareFileEntryStoreImpl(
                shareEntriesFileProvider,
                fileOperations,
                serdeOperations
        );
    }

    @Bean
    public HandshakeTrustedOutStore handshakeTrustedOutStore(FileProvider trustOutFileProvider,
                                                             CryptoFileOperations fileOperations,
                                                             ObjectMapper objectMapper) {
        SerdeOperations<HandshakeTrustedOutStore.TrustedOut> serdeOperations = new JsonSerdeOperations<>(
                objectMapper,
                HandshakeTrustedOutStore.TrustedOut.class
        );
        return new HandshakeTrustedOutStoreImpl(
                trustOutFileProvider,
                fileOperations,
                serdeOperations
        );
    }

    @Bean
    public HandshakeTrustedInStore handshakeTrustedInStore(FileProvider trustInFileProvider,
                                                           CryptoFileOperations fileOperations,
                                                           ObjectMapper objectMapper) {
        SerdeOperations<HandshakeTrustedInStore.TrustedIn> serdeOperations = new JsonSerdeOperations<>(
                objectMapper,
                HandshakeTrustedInStore.TrustedIn.class
        );
        return new HandshakeTrustedInStoreImpl(
                trustInFileProvider,
                fileOperations,
                serdeOperations
        );
    }

    @Bean
    public HandshakeSessionOutStore handshakeSessionOutStore() {
        return new RuntimeHandshakeSessionOutStore();
    }

    @Bean
    public HandshakeSessionInStore handshakeSessionInStore() {
        return new RuntimeHandshakeSessionInStore();
    }

    @Bean
    public QuickShareEntryStore linkShareEntryStore() {
        return new RuntimeQuickShareEntryStore();
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
                        daemonSecretFileProvider,
                        fileOperations,
                        serdeOperations
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
        return new InstallationSeedBootstrapStoreImpl(
                new CacheFileKeyValueStore<>(
                        installationSeedFileProvider,
                        fileOperations,
                        serdeOperations
                )
        );
    }
}
