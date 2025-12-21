package com.evolution.dropfilecli.config;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.configuration.app.DropFileAppConfig;
import com.evolution.dropfile.configuration.app.DropFileAppConfigStore;
import com.evolution.dropfile.configuration.app.ImmutableDropFileAppConfigStore;
import com.evolution.dropfile.configuration.secret.DropFileSecretsConfig;
import com.evolution.dropfile.configuration.secret.DropFileSecretsConfigStore;
import com.evolution.dropfile.configuration.secret.ImmutableDropFileSecretsConfigStore;
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
    public DropFileAppConfigStore appConfigStore(Environment environment) {
        return new ImmutableDropFileAppConfigStore(() -> {
            String daemonHost = environment.getRequiredProperty("dropfile.daemon.host");
            Integer daemonPort = Integer.valueOf(environment.getRequiredProperty("dropfile.daemon.port"));

            String daemonPublicAddress = environment.getProperty("dropfile.daemon.public.address");

            return new DropFileAppConfig(
                    new DropFileAppConfig.DropFileCliAppConfig(
                            daemonHost,
                            daemonPort
                    ),
                    new DropFileAppConfig.DropFileDaemonAppConfig(
                            "NO-SET",
                            daemonPort,
                            Optional.ofNullable(daemonPublicAddress).map(it -> CommonUtils.toURI(it)).orElse(null)
                    )
            );
        });
    }

    @Bean
    public DropFileSecretsConfigStore secretsConfigStore(Environment environment) {
        return new ImmutableDropFileSecretsConfigStore(() -> {
            String daemonToken = environment.getRequiredProperty("dropfile.daemon.token");

            return new DropFileSecretsConfig(daemonToken);
        });
    }
}
