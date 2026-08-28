package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.common.LockableOperation;
import com.evolution.dropfile.common.SystemInfoProvider;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.common.crypto.CryptoTunnelChaCha20Poly1305;
import com.evolution.dropfile.common.io.FileHelper;
import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfile.store.access.RuntimeAccessKeyStore;
import com.evolution.dropfile.store.download.DownloadFile;
import com.evolution.dropfile.store.download.FileDownloadStore;
import com.evolution.dropfile.store.download.FileDownloadStoreCacheable;
import com.evolution.dropfile.store.framework.file.*;
import com.evolution.dropfile.store.quickshare.QuickShareStore;
import com.evolution.dropfile.store.quickshare.RuntimeQuickShareStore;
import com.evolution.dropfile.store.secret.DaemonSecret;
import com.evolution.dropfile.store.secret.DaemonSecretStore;
import com.evolution.dropfile.store.secret.DaemonSecretStoreCacheable;
import com.evolution.dropfile.store.seed.InstallationSeedBootstrapStore;
import com.evolution.dropfile.store.seed.InstallationSeedBootstrapStoreCacheable;
import com.evolution.dropfile.store.share.ShareFile;
import com.evolution.dropfile.store.share.ShareFileStore;
import com.evolution.dropfile.store.share.ShareFileStoreCacheable;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStoreCacheable;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStoreCacheable;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.net.http.HttpClient;
import java.nio.file.Paths;
import java.util.UUID;

@Configuration
public class DropFileDaemonConfiguration {

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
        objectMapper.registerModule(new JavaTimeModule());
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

    @Bean
    public DirectoryProvider daemonDownloadsDirectoryProvider(DaemonApplicationProperties applicationProperties) {
        return new DirectoryProviderImpl(applicationProperties.daemonDownloadsDirectory);
    }

    @Bean
    public LockableOperation lockableOperationHandshakeTrustedInStore(HandshakeTrustedInStore store) {
        return new LockableOperation(key -> {
            return store.get(key).isEmpty();
        });
    }

    @Bean
    public LockableOperation lockableOperationHandshakeTrustedOutStore(HandshakeTrustedOutStore store) {
        return new LockableOperation(key -> {
            return store.get(key).isEmpty();
        });
    }

    @Bean
    public DirectoryProvider daemonApplicationHomeDirectoryProvider(DaemonApplicationProperties applicationProperties) {
        return new DirectoryProviderImpl(applicationProperties.daemonApplicationHomeDirectory);
    }

    @Bean
    public DirectoryProvider daemonSecretsDirectoryProvider(DaemonApplicationProperties applicationProperties) {
        return new DirectoryProviderImpl(applicationProperties.daemonSecretsDirectory);
    }

    @Bean
    public DirectoryProvider daemonInstallationSeedDirectoryProvider(DaemonApplicationProperties applicationProperties) {
        return new DirectoryProviderImpl(applicationProperties.daemonInstallationSeedDirectory);
    }

    @Bean
    public DirectoryProvider daemonConfigDirectoryProvider(DirectoryProvider daemonApplicationHomeDirectoryProvider) {
        return new DirectoryProviderImpl(daemonApplicationHomeDirectoryProvider, Paths.get("conf"));
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
        return new FileProviderImpl(daemonInstallationSeedDirectoryProvider, Paths.get(".installation.json"));
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
    public FileDownloadStore fileDownloadEntryStore(FileProvider downloadEntriesFileProvider,
                                                    FileOperations fileOperations,
                                                    ObjectMapper objectMapper) {
        SerdeOperations<DownloadFile> serdeOperations = new JsonSerdeOperations<>(
                objectMapper,
                DownloadFile.class
        );
        return new FileDownloadStoreCacheable(
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
    public ShareFileStore shareFileEntryStore(FileProvider shareEntriesFileProvider,
                                              FileOperations fileOperations,
                                              ObjectMapper objectMapper) {
        SerdeOperations<ShareFile> serdeOperations = new JsonSerdeOperations<>(
                objectMapper,
                ShareFile.class
        );
        return new ShareFileStoreCacheable(
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
        return new HandshakeTrustedOutStoreCacheable(
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
        return new HandshakeTrustedInStoreCacheable(
                trustInFileProvider,
                fileOperations,
                serdeOperations
        );
    }

    @Bean
    public QuickShareStore linkShareEntryStore() {
        return new RuntimeQuickShareStore();
    }

    @Bean
    public DaemonSecretStore daemonSecretStore(FileProvider daemonSecretFileProvider,
                                               CryptoFileOperations fileOperations,
                                               ObjectMapper objectMapper) {
        SerdeOperations<DaemonSecret> serdeOperations = new JsonSerdeOperations<>(
                objectMapper,
                DaemonSecret.class
        );
        return new DaemonSecretStoreCacheable(
                new CacheableFileKeyValueStore<>(
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
        return new InstallationSeedBootstrapStoreCacheable(
                new CacheableFileKeyValueStore<>(
                        installationSeedFileProvider,
                        fileOperations,
                        serdeOperations
                )
        );
    }
}