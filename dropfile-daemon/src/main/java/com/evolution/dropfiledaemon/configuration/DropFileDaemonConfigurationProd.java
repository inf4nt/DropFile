package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfile.store.access.RuntimeAccessKeyStore;
import com.evolution.dropfile.store.download.FileDownloadEntryStore;
import com.evolution.dropfile.store.download.JsonFileFileDownloadEntryStore;
import com.evolution.dropfile.store.framework.file.ApplicationFingerprintSupplier;
import com.evolution.dropfile.store.link.LinkShareEntryStore;
import com.evolution.dropfile.store.link.RuntimeLinkShareEntryStore;
import com.evolution.dropfile.store.secret.CryptoDaemonSecretsStore;
import com.evolution.dropfile.store.secret.DaemonSecretsStore;
import com.evolution.dropfile.store.share.JsonFileShareFileEntryStore;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
import com.evolution.dropfiledaemon.configuration.middleware.DaemonSecretsSingleValueStoreInitializationProcedure;
import com.evolution.dropfiledaemon.configuration.middleware.FileDownloadEntryStoreKeyValueStoreInitializationProcedure;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionInStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionOutStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.handshake.store.crypto.CryptoHandshakeTrustedInStore;
import com.evolution.dropfiledaemon.handshake.store.crypto.CryptoHandshakeTrustedOutStore;
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
    public FileDownloadEntryStore fileDownloadEntryStore(FileHelper fileHelper,
                                                         ObjectMapper objectMapper,
                                                         DaemonApplicationProperties daemonApplicationProperties) {
        return new JsonFileFileDownloadEntryStore(fileHelper,
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
        return new JsonFileShareFileEntryStore(fileHelper,
                objectMapper,
                Paths.get(daemonApplicationProperties.configDirectory)
        );
    }

    @Bean
    public HandshakeTrustedOutStore handshakeTrustedOutStore(FileHelper fileHelper,
                                                             ObjectMapper objectMapper,
                                                             CryptoTunnel cryptoTunnel,
                                                             ApplicationFingerprintSupplier applicationFingerprintSupplier,
                                                             DaemonApplicationProperties daemonApplicationProperties) {
        return new CryptoHandshakeTrustedOutStore(
                fileHelper,
                objectMapper,
                cryptoTunnel,
                applicationFingerprintSupplier,
                Paths.get(daemonApplicationProperties.configDirectory)
        );
    }

    @Bean
    public HandshakeTrustedInStore handshakeTrustedInStore(FileHelper fileHelper,
                                                           ObjectMapper objectMapper,
                                                           CryptoTunnel cryptoTunnel,
                                                           ApplicationFingerprintSupplier applicationFingerprintSupplier,
                                                           DaemonApplicationProperties daemonApplicationProperties) {
        return new CryptoHandshakeTrustedInStore(
                fileHelper,
                objectMapper,
                cryptoTunnel,
                applicationFingerprintSupplier,
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
                                                 ApplicationFingerprintSupplier applicationFingerprintSupplier,
                                                 DaemonApplicationProperties daemonApplicationProperties) {
        return new CryptoDaemonSecretsStore(
                fileHelper,
                objectMapper,
                cryptoTunnel,
                applicationFingerprintSupplier,
                Paths.get(daemonApplicationProperties.configDirectory)
        );
    }

    @Bean
    public DaemonSecretsSingleValueStoreInitializationProcedure daemonSecretsSingleValueStoreInitializationProcedure() {
        return new DaemonSecretsSingleValueStoreInitializationProcedure();
    }
}
