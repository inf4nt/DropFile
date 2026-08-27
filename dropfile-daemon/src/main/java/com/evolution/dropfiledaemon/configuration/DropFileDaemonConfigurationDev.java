package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfile.store.access.RuntimeAccessKeyStore;
import com.evolution.dropfile.store.download.FileDownloadStore;
import com.evolution.dropfile.store.download.RuntimeFileDownloadStore;
import com.evolution.dropfile.store.quickshare.QuickShareStore;
import com.evolution.dropfile.store.quickshare.RuntimeQuickShareStore;
import com.evolution.dropfile.store.secret.DaemonSecret;
import com.evolution.dropfile.store.secret.DaemonSecretStore;
import com.evolution.dropfile.store.secret.ImmutableDaemonSecretStore;
import com.evolution.dropfile.store.share.RuntimeShareFileStore;
import com.evolution.dropfile.store.share.ShareFileStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.handshake.store.runtime.RuntimeHandshakeTrustedInStore;
import com.evolution.dropfiledaemon.handshake.store.runtime.RuntimeHandshakeTrustedOutStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@Profile("dev")
@Configuration
public class DropFileDaemonConfigurationDev {

    @Bean
    public FileDownloadStore fileDownloadEntryStore() {
        return new RuntimeFileDownloadStore();
    }

    @Bean
    public AccessKeyStore accessKeyStore() {
        return new RuntimeAccessKeyStore();
    }

    @Bean
    public ShareFileStore shareFileEntryStore() {
        return new RuntimeShareFileStore();
    }

    @Bean
    public HandshakeTrustedOutStore handshakeTrustedOutStore() {
        return new RuntimeHandshakeTrustedOutStore();
    }

    @Bean
    public HandshakeTrustedInStore handshakeTrustedInStore() {
        return new RuntimeHandshakeTrustedInStore();
    }

    @Bean
    public QuickShareStore linkShareEntryStore() {
        return new RuntimeQuickShareStore();
    }

    @Bean
    public DaemonSecretStore daemonSecretStore(@Value("${dropfile.daemon.token}") String daemonToken) {
        log.info("Provided daemon token: {}", daemonToken);
        DaemonSecret secrets = new DaemonSecret(daemonToken);
        return new ImmutableDaemonSecretStore(secrets);
    }
}
