package com.evolution.dropfilecli.config;

import com.evolution.dropfile.store.secret.DaemonSecrets;
import com.evolution.dropfile.store.secret.DaemonSecretsStore;
import com.evolution.dropfile.store.secret.ImmutableDaemonSecretsStore;
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
    public DaemonSecretsStore secretsConfigStore(@Value("${dropfile.daemon.token}") String daemonToken) {
        DaemonSecrets secrets = new DaemonSecrets(daemonToken);
        return new ImmutableDaemonSecretsStore(secrets);
    }
}
