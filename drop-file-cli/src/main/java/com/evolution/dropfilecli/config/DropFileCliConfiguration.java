package com.evolution.dropfilecli.config;

import com.evolution.dropfile.configuration.app.DropFileAppConfig;
import com.evolution.dropfile.configuration.app.DropFileAppConfigFacade;
import com.evolution.dropfile.configuration.secret.DropFileSecretsConfig;
import com.evolution.dropfile.configuration.secret.DropFileSecretsConfigFacade;
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
    public DropFileAppConfigFacade appConfigFacade(ObjectMapper objectMapper) {
        return new DropFileAppConfigFacade(objectMapper);
    }

    @Bean
    public DropFileSecretsConfigFacade secretsConfigFacade(ObjectMapper objectMapper) {
        return new DropFileSecretsConfigFacade(objectMapper);
    }

    @Bean
    public DropFileAppConfig appConfig(DropFileAppConfigFacade dropFileAppConfigFacade) {
        return dropFileAppConfigFacade.get();
    }

    @Bean
    public DropFileSecretsConfig secretsConfig(DropFileSecretsConfigFacade dropFileSecretsConfigFacade) {
        return dropFileSecretsConfigFacade.get();
    }
}
