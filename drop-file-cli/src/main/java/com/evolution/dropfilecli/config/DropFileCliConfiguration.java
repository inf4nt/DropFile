package com.evolution.dropfilecli.config;

import com.evolution.dropfile.configuration.app.DropFileAppConfigManager;
import com.evolution.dropfile.configuration.secret.DropFileSecretsConfigManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

@Configuration
public class DropFileCliConfiguration {

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder().build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public DropFileAppConfigManager appConfigFacade(ObjectMapper objectMapper) {
        return new DropFileAppConfigManager(objectMapper);
    }

    @Bean
    public DropFileSecretsConfigManager secretsConfigFacade(ObjectMapper objectMapper) {
        return new DropFileSecretsConfigManager(objectMapper);
    }
}
