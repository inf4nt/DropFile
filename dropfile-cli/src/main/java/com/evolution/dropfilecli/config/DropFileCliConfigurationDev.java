package com.evolution.dropfilecli.config;

import com.evolution.dropfile.store.secret.DaemonSecrets;
import com.evolution.dropfile.store.secret.DaemonSecretsStore;
import com.evolution.dropfile.store.secret.ImmutableDaemonSecretsStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Slf4j
@Profile("dev")
@Configuration
public class DropFileCliConfigurationDev {

    @Bean
    public DaemonSecretsStore secretsConfigStore(Environment environment) {
        return new ImmutableDaemonSecretsStore(() -> {
            String daemonToken = environment.getRequiredProperty("dropfile.daemon.token");

            return new DaemonSecrets(daemonToken);
        });
    }
}
