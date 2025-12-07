package com.evolution.dropfilecli.config;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.configuration.app.DropFileAppConfig;
import com.evolution.dropfile.configuration.app.DropFileAppConfigManager;
import com.evolution.dropfile.configuration.secret.DropFileSecretsConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.File;

@Slf4j
@Profile("dev")
@Configuration
public class DropFileCliConfigurationDev {

    @Bean
    public DropFileAppConfig.DropFileCliAppConfig appCliConfig(@Value("${dropfile.daemon.host}") String daemonHost,
                                                               @Value("${dropfile.daemon.port}") Integer daemonPort) {
        log.info("Provided daemon host: {}", daemonHost);
        log.info("Provided daemon port: {}", daemonPort);
        return new DropFileAppConfig.DropFileCliAppConfig(daemonHost, daemonPort);
    }

    @Bean
    public DropFileAppConfig.DropFileDaemonAppConfig daemonAppConfig(@Value("${dropfile.daemon.public.address}") String daemonPublicAddress,
                                                                     DropFileAppConfig.DropFileCliAppConfig cliAppConfig) {
        log.info("Provided daemon public address: {}", daemonPublicAddress);
        return new DropFileAppConfig.DropFileDaemonAppConfig(
                "NO-SET",
                cliAppConfig.getDaemonPort(),
                CommonUtils.toURI(daemonPublicAddress)
        );
    }

    @Bean
    public DropFileSecretsConfig secretsConfig(@Value("${dropfile.daemon.token}") String daemonSecret) {
        log.info("Provided daemon secret: {}", daemonSecret);
        return new DropFileSecretsConfig(daemonSecret);
    }

    @Bean
    public DropFileAppConfigManager appConfigManager(ObjectMapper objectMapper) {
        return new DropFileAppConfigManager(objectMapper) {
            @Override
            public DropFileAppConfig get() {
                throw new UnsupportedOperationException("Dev profile does not support it");
            }

            @Override
            public DropFileAppConfig save(DropFileAppConfig config) {
                throw new UnsupportedOperationException("Dev profile does not support it");
            }

            @Override
            public DropFileAppConfig read(File file) {
                throw new UnsupportedOperationException("Dev profile does not support it");
            }
        };
    }
}
