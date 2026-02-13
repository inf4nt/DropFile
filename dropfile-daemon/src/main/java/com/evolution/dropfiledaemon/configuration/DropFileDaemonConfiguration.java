package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfiledaemon.tunnel.CryptoTunnel;
import com.evolution.dropfiledaemon.tunnel.CryptoTunnelChaCha20Poly1305;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

@Configuration
public class DropFileDaemonConfiguration {

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder().build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    @Bean
    public CryptoTunnel cryptoTunnel() {
        return new CryptoTunnelChaCha20Poly1305();
    }
}