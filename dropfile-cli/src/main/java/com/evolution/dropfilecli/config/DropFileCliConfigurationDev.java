package com.evolution.dropfilecli.config;

import com.evolution.dropfile.store.app.AppConfig;
import com.evolution.dropfile.store.app.AppConfigStore;
import com.evolution.dropfile.store.app.ImmutableAppConfigStore;
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
    public AppConfigStore appConfigStore(Environment environment) {
        return new ImmutableAppConfigStore(() -> {
            String daemonHost = environment.getRequiredProperty("dropfile.daemon.host");
            int daemonPort = Integer.parseInt(environment.getRequiredProperty("dropfile.daemon.port"));

            return new AppConfig(
                    new AppConfig.CliAppConfig(
                            daemonHost,
                            daemonPort
                    ),
                    new AppConfig.DaemonAppConfig(
                            "NO-SET",
                            daemonPort,
                            null,
                            null
                    )
            );
        });
    }

    @Bean
    public DaemonSecretsStore secretsConfigStore(Environment environment) {
        return new ImmutableDaemonSecretsStore(() -> {
            String daemonToken = environment.getRequiredProperty("dropfile.daemon.token");

            return new DaemonSecrets(daemonToken);
        });
    }
}
