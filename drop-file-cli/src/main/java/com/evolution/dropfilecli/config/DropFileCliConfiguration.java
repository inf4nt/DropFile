package com.evolution.dropfilecli.config;

import com.evolution.dropfile.configuration.app.DropFileAppConfig;
import com.evolution.dropfile.configuration.app.DropFileAppConfigManager;
import com.evolution.dropfile.configuration.secret.DropFileSecretsConfig;
import com.evolution.dropfile.configuration.secret.DropFileSecretsConfigManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.net.http.HttpClient;

@Configuration
public class DropFileCliConfiguration {

    @Value("${config.app:#{null}}")
    private String customAppConfig;

    @Value("${config.secrets:#{null}}")
    private String customSecretsConfig;

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder().build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public DropFileAppConfigManager appConfigManager(ObjectMapper objectMapper) {
        return new DropFileAppConfigManager(objectMapper);
    }

    @Bean
    public DropFileSecretsConfigManager secretsConfigManager(ObjectMapper objectMapper) {
        return new DropFileSecretsConfigManager(objectMapper);
    }

    @Bean
    public DropFileAppConfig appConfig(DropFileAppConfigManager appConfig) {
        if (!ObjectUtils.isEmpty(customAppConfig)) {
            return appConfig.read(new File(customAppConfig));
        }
        return appConfig.get();
    }

    @Bean
    public DropFileSecretsConfig secretsConfig(DropFileSecretsConfigManager secretsConfig) {
        if (!ObjectUtils.isEmpty(customSecretsConfig)) {
            return secretsConfig.read(new File(customSecretsConfig));
        }
        return secretsConfig.get();
    }
}
