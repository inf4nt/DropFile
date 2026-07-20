package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfile.store.access.RuntimeAccessKeyStore;
import com.evolution.dropfile.store.download.FileDownloadEntryStore;
import com.evolution.dropfile.store.download.RuntimeFileDownloadEntryStore;
import com.evolution.dropfile.store.quickshare.QuickShareEntryStore;
import com.evolution.dropfile.store.quickshare.RuntimeQuickShareEntryStore;
import com.evolution.dropfile.store.secret.DaemonSecrets;
import com.evolution.dropfile.store.secret.DaemonSecretsStore;
import com.evolution.dropfile.store.secret.ImmutableDaemonSecretsStore;
import com.evolution.dropfile.store.share.RuntimeShareFileEntryStore;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
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
    public FileDownloadEntryStore fileDownloadEntryStore() {
        return new RuntimeFileDownloadEntryStore();
    }

    @Bean
    public AccessKeyStore accessKeyStore() {
        return new RuntimeAccessKeyStore();
    }

    @Bean
    public ShareFileEntryStore shareFileEntryStore() {
        return new RuntimeShareFileEntryStore();
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
    public QuickShareEntryStore linkShareEntryStore() {
        return new RuntimeQuickShareEntryStore();
    }

    @Bean
    public DaemonSecretsStore daemonSecretsStore(@Value("${dropfile.daemon.token}") String daemonToken) {
        log.info("Provided daemon token: {}", daemonToken);
        DaemonSecrets secrets = new DaemonSecrets(daemonToken);
        return new ImmutableDaemonSecretsStore(secrets);
    }
}
