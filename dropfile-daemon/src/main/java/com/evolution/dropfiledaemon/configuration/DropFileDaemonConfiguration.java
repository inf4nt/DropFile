package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfiledaemon.handshake.store.HandshakeStore;
import com.evolution.dropfiledaemon.handshake.store.runtime.RuntimeTrustedInKeyValueStore;
import com.evolution.dropfiledaemon.handshake.store.runtime.RuntimeTrustedOutKeyValueStore;
import com.evolution.dropfiledaemon.handshake.store.runtime.RuntimeIncomingRequestKeyValueStore;
import com.evolution.dropfiledaemon.handshake.store.runtime.RuntimeOutgoingRequestKeyValueStore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return objectMapper;
    }

    @Bean
    public HandshakeStore handshakeStore() {
        return new HandshakeStore(
                new RuntimeIncomingRequestKeyValueStore(),
                new RuntimeOutgoingRequestKeyValueStore(),
                new RuntimeTrustedInKeyValueStore(),
                new RuntimeTrustedOutKeyValueStore()
        );
    }
}