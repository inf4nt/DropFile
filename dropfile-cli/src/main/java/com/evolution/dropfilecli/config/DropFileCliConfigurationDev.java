package com.evolution.dropfilecli.config;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.configuration.app.AppConfig;
import com.evolution.dropfile.configuration.app.AppConfigStore;
import com.evolution.dropfile.configuration.app.ImmutableAppConfigStore;
import com.evolution.dropfile.configuration.secret.SecretsConfig;
import com.evolution.dropfile.configuration.secret.SecretsConfigStore;
import com.evolution.dropfile.configuration.secret.ImmutableSecretsConfigStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.util.Optional;

@Slf4j
@Profile("dev")
@Configuration
public class DropFileCliConfigurationDev {

    @Bean
    public AppConfigStore appConfigStore(Environment environment) {
        return new ImmutableAppConfigStore(() -> {
            String daemonHost = environment.getRequiredProperty("dropfile.daemon.host");
            Integer daemonPort = Integer.valueOf(environment.getRequiredProperty("dropfile.daemon.port"));

            String daemonPublicAddress = environment.getProperty("dropfile.daemon.public.address");

            return new AppConfig(
                    new AppConfig.CliAppConfig(
                            daemonHost,
                            daemonPort
                    ),
                    new AppConfig.DaemonAppConfig(
                            "NO-SET",
                            daemonPort,
                            Optional.ofNullable(daemonPublicAddress).map(it -> CommonUtils.toURI(it)).orElse(null)
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
