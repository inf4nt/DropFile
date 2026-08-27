package com.evolution.dropfilecli.config;

import com.evolution.dropfile.store.secret.DaemonSecret;
import com.evolution.dropfile.store.secret.DaemonSecretStore;
import com.evolution.dropfile.store.secret.ImmutableDaemonSecretStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@Profile("dev")
@Configuration
public class DropFileCliConfigurationDev {

    @Bean
    public DaemonSecretStore daemonSecretStore(@Value("${dropfile.daemon.token}") String daemonToken) {
        DaemonSecret secrets = new DaemonSecret(daemonToken);
        return new ImmutableDaemonSecretStore(secrets);
    }
}
