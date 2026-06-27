package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfile.store.access.RuntimeAccessKeyStore;
import com.evolution.dropfile.store.download.DownloadFileEntry;
import com.evolution.dropfile.store.download.FileDownloadEntryStore;
import com.evolution.dropfile.store.download.FileDownloadEntryStoreImpl;
import com.evolution.dropfile.store.framework.file.*;
import com.evolution.dropfile.store.link.LinkShareEntryStore;
import com.evolution.dropfile.store.link.RuntimeLinkShareEntryStore;
import com.evolution.dropfile.store.secret.DaemonSecrets;
import com.evolution.dropfile.store.secret.DaemonSecretsStore;
import com.evolution.dropfile.store.secret.DaemonSecretsStoreImpl;
import com.evolution.dropfile.store.share.ShareFileEntry;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
import com.evolution.dropfile.store.share.ShareFileEntryStoreImpl;
import com.evolution.dropfiledaemon.bootstrap.middleware.DaemonSecretsSingleValueStoreInitializationProcedure;
import com.evolution.dropfiledaemon.bootstrap.middleware.FileDownloadEntryStoreKeyValueStoreInitializationProcedure;
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

@Slf4j
@Profile("prod")
@Configuration
public class DropFileDaemonConfigurationProd {

    @Bean
    public InstallationSeedProvider installationSeedProvider(DaemonApplicationProperties applicationProperties) {
        FileProvider fileProvider = new FileProviderImpl(
                Paths.get(applicationProperties.configDirectory),
                ".installation.bin"
        );
        return new FileSystemInstallationSeedProvider(fileProvider);
    }

    @Primary
    @Bean
    public FileSystemOperations fileSystemOperations(FileHelper fileHelper) {
        return new FileSystemOperations(fileHelper);
    }

    @Bean
    public CryptoFileOperations cryptoFileOperations(FileOperations fileOperations,
                                                     CryptoTunnel cryptoTunnel,
                                                     InstallationSeedProvider installationSeedProvider) {
        return new CryptoFileOperations(
                fileOperations,
                cryptoTunnel,
                installationSeedProvider
        );
    }

    @Bean
    public FileDownloadEntryStore fileDownloadEntryStore(FileOperations fileOperations,
                                                         ObjectMapper objectMapper,
                                                         DaemonApplicationProperties daemonApplicationProperties) {
        FileProvider fileProvider = new FileProviderImpl(
                Paths.get(daemonApplicationProperties.configDirectory),
                "download.entries.json"
        );
        SerdeOperations<DownloadFileEntry> serdeOperations = new JsonSerdeOperations<>(
                objectMapper,
                DownloadFileEntry.class
        );
        return new FileDownloadEntryStoreImpl(
                fileProvider,
                fileOperations,
                serdeOperations
        );
    }

    @Bean
    public FileDownloadEntryStoreKeyValueStoreInitializationProcedure fileDownloadEntryStoreKeyValueStoreInitializationProcedure(FileDownloadEntryStore store) {
        return new FileDownloadEntryStoreKeyValueStoreInitializationProcedure(store);
    }

    @Bean
    public AccessKeyStore accessKeyStore() {
        return new RuntimeAccessKeyStore();
    }

    @Bean
    public ShareFileEntryStore shareFileEntryStore(FileOperations fileOperations,
                                                   ObjectMapper objectMapper,
                                                   DaemonApplicationProperties daemonApplicationProperties) {
        FileProvider fileProvider = new FileProviderImpl(
                Paths.get(daemonApplicationProperties.configDirectory),
                "share.entries.json"
        );
        SerdeOperations<ShareFileEntry> serdeOperations = new JsonSerdeOperations<>(
                objectMapper,
                ShareFileEntry.class
        );
        return new ShareFileEntryStoreImpl(
                fileProvider,
                fileOperations,
                serdeOperations
        );
    }

    @Bean
    public HandshakeTrustedOutStore handshakeTrustedOutStore(CryptoFileOperations fileOperations,
                                                             ObjectMapper objectMapper,
                                                             DaemonApplicationProperties daemonApplicationProperties) {
        FileProvider fileProvider = new FileProviderImpl(
                Paths.get(daemonApplicationProperties.configDirectory),
                ".trustout.bin"
        );
        SerdeOperations<HandshakeTrustedOutStore.TrustedOut> serdeOperations = new JsonSerdeOperations<>(
                objectMapper,
                HandshakeTrustedOutStore.TrustedOut.class
        );
        return new HandshakeTrustedOutStoreImpl(
                fileProvider,
                fileOperations,
                serdeOperations
        );
    }

    @Bean
    public HandshakeTrustedInStore handshakeTrustedInStore(CryptoFileOperations fileOperations,
                                                           ObjectMapper objectMapper,
                                                           DaemonApplicationProperties daemonApplicationProperties) {
        FileProvider fileProvider = new FileProviderImpl(
                Paths.get(daemonApplicationProperties.configDirectory),
                ".trustin.bin"
        );
        SerdeOperations<HandshakeTrustedInStore.TrustedIn> serdeOperations = new JsonSerdeOperations<>(
                objectMapper,
                HandshakeTrustedInStore.TrustedIn.class
        );
        return new HandshakeTrustedInStoreImpl(
                fileProvider,
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
    public LinkShareEntryStore linkShareEntryStore() {
        return new RuntimeLinkShareEntryStore();
    }

    @Bean
    public DaemonSecretsStore daemonSecretsStore(CryptoFileOperations fileOperations,
                                                 ObjectMapper objectMapper,
                                                 DaemonApplicationProperties daemonApplicationProperties) {
        FileProvider fileProvider = new FileProviderImpl(
                Paths.get(daemonApplicationProperties.configDirectory),
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
    public DaemonSecretsSingleValueStoreInitializationProcedure daemonSecretsSingleValueStoreInitializationProcedure(DaemonSecretsStore store) {
        return new DaemonSecretsSingleValueStoreInitializationProcedure(store);
    }
}
