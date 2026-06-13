package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfile.store.access.RuntimeAccessKeyStore;
import com.evolution.dropfile.store.download.FileDownloadEntryStore;
import com.evolution.dropfile.store.download.RuntimeFileDownloadEntryStore;
import com.evolution.dropfile.store.link.LinkShareEntryStore;
import com.evolution.dropfile.store.link.RuntimeLinkShareEntryStore;
import com.evolution.dropfile.store.secret.DaemonSecrets;
import com.evolution.dropfile.store.secret.DaemonSecretsStore;
import com.evolution.dropfile.store.secret.ImmutableDaemonSecretsStore;
import com.evolution.dropfile.store.share.RuntimeShareFileEntryStore;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionInStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionOutStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.handshake.store.runtime.RuntimeHandshakeSessionInStore;
import com.evolution.dropfiledaemon.handshake.store.runtime.RuntimeHandshakeSessionOutStore;
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
    public DaemonSecretsStore daemonSecretsStore(@Value("${dropfile.daemon.token}") String daemonToken) {
        log.info("Provided daemon token: {}", daemonToken);
        return new ImmutableDaemonSecretsStore(() -> new DaemonSecrets(daemonToken));
    }
}
