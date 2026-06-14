package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfile.store.access.RuntimeAccessKeyStore;
import com.evolution.dropfile.store.download.CacheDownloadEntryStore;
import com.evolution.dropfile.store.download.FileDownloadEntryStore;
import com.evolution.dropfile.store.framework.file.InstallationSeedProvider;
import com.evolution.dropfile.store.framework.file.FileSystemInstallationSeedProvider;
import com.evolution.dropfile.store.framework.file.FileProvider;
import com.evolution.dropfile.store.framework.file.FileProviderImpl;
import com.evolution.dropfile.store.link.LinkShareEntryStore;
import com.evolution.dropfile.store.link.RuntimeLinkShareEntryStore;
import com.evolution.dropfile.store.secret.CryptoCacheDaemonSecretsStore;
import com.evolution.dropfile.store.secret.DaemonSecretsStore;
import com.evolution.dropfile.store.share.CacheShareFileEntryStore;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
import com.evolution.dropfiledaemon.configuration.middleware.DaemonSecretsSingleValueStoreInitializationProcedure;
import com.evolution.dropfiledaemon.configuration.middleware.FileDownloadEntryStoreKeyValueStoreInitializationProcedure;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionInStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionOutStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.handshake.store.crypto.CryptoCacheHandshakeTrustedInStore;
import com.evolution.dropfiledaemon.handshake.store.crypto.CryptoCacheHandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.handshake.store.runtime.RuntimeHandshakeSessionInStore;
import com.evolution.dropfiledaemon.handshake.store.runtime.RuntimeHandshakeSessionOutStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.nio.file.Paths;

@Slf4j
@Profile("prod")
@Configuration
public class DropFileDaemonConfigurationProd {

    @Bean
    public InstallationSeedProvider applicationFingerprintSupplier(DaemonApplicationProperties applicationProperties) {
        FileProvider fileProvider = new FileProviderImpl(
                Paths.get(applicationProperties.configDirectory),
                ".fingerprint.bin"
        );
        return new FileSystemInstallationSeedProvider(fileProvider);
    }

    @Bean
    public FileDownloadEntryStore fileDownloadEntryStore(FileHelper fileHelper,
                                                         ObjectMapper objectMapper,
                                                         DaemonApplicationProperties daemonApplicationProperties) {


        return new CacheDownloadEntryStore(fileHelper,
                objectMapper,
                Paths.get(daemonApplicationProperties.configDirectory)
        );
    }

    @Bean
    public FileDownloadEntryStoreKeyValueStoreInitializationProcedure fileDownloadEntryStoreKeyValueStoreInitializationProcedure() {
        return new FileDownloadEntryStoreKeyValueStoreInitializationProcedure();
    }

    @Bean
    public AccessKeyStore accessKeyStore() {
        return new RuntimeAccessKeyStore();
    }

    @Bean
    public ShareFileEntryStore shareFileEntryStore(FileHelper fileHelper,
                                                   ObjectMapper objectMapper,
                                                   DaemonApplicationProperties daemonApplicationProperties) {
        return new CacheShareFileEntryStore(fileHelper,
                objectMapper,
                Paths.get(daemonApplicationProperties.configDirectory)
        );
    }

    @Bean
    public HandshakeTrustedOutStore handshakeTrustedOutStore(FileHelper fileHelper,
                                                             ObjectMapper objectMapper,
                                                             CryptoTunnel cryptoTunnel,
                                                             InstallationSeedProvider installationSeedProvider,
                                                             DaemonApplicationProperties daemonApplicationProperties) {
        return new CryptoCacheHandshakeTrustedOutStore(
                fileHelper,
                objectMapper,
                cryptoTunnel,
                installationSeedProvider,
                Paths.get(daemonApplicationProperties.configDirectory)
        );
    }

    @Bean
    public HandshakeTrustedInStore handshakeTrustedInStore(FileHelper fileHelper,
                                                           ObjectMapper objectMapper,
                                                           CryptoTunnel cryptoTunnel,
                                                           InstallationSeedProvider installationSeedProvider,
                                                           DaemonApplicationProperties daemonApplicationProperties) {
        return new CryptoCacheHandshakeTrustedInStore(
                fileHelper,
                objectMapper,
                cryptoTunnel,
                installationSeedProvider,
                Paths.get(daemonApplicationProperties.configDirectory)
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
    public DaemonSecretsStore daemonSecretsStore(FileHelper fileHelper,
                                                 ObjectMapper objectMapper,
                                                 CryptoTunnel cryptoTunnel,
                                                 InstallationSeedProvider installationSeedProvider,
                                                 DaemonApplicationProperties daemonApplicationProperties) {
        return new CryptoCacheDaemonSecretsStore(
                fileHelper,
                objectMapper,
                cryptoTunnel,
                installationSeedProvider,
                Paths.get(daemonApplicationProperties.configDirectory)
        );
    }

    @Bean
    public DaemonSecretsSingleValueStoreInitializationProcedure daemonSecretsSingleValueStoreInitializationProcedure() {
        return new DaemonSecretsSingleValueStoreInitializationProcedure();
    }
}
