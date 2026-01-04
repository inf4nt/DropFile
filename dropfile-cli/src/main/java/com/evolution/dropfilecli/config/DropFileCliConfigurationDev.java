package com.evolution.dropfilecli.config;

import com.evolution.dropfile.configuration.app.AppConfig;
import com.evolution.dropfile.configuration.app.AppConfigStore;
import com.evolution.dropfile.configuration.app.ImmutableAppConfigStore;
import com.evolution.dropfile.configuration.secret.ImmutableSecretsConfigStore;
import com.evolution.dropfile.configuration.secret.SecretsConfig;
import com.evolution.dropfile.configuration.secret.SecretsConfigStore;
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
            Integer daemonPort = Integer.valueOf(environment.getRequiredProperty("dropfile.daemon.port"));

            return new AppConfig(
                    new AppConfig.CliAppConfig(
                            daemonHost,
                            daemonPort
                    ),
                    new AppConfig.DaemonAppConfig(
                            "NO-SET",
                            daemonPort
                    )
            );
        });
    }

    @Bean
    public SecretsConfigStore secretsConfigStore(Environment environment) {
        return new ImmutableSecretsConfigStore(() -> {
            String daemonToken = environment.getRequiredProperty("dropfile.daemon.token");

            return new SecretsConfig(daemonToken);
        });
    }
}
