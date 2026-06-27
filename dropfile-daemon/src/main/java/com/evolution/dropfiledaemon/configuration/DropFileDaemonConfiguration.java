package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.common.crypto.CryptoTunnelChaCha20Poly1305;
import com.evolution.dropfile.store.framework.KeyValueStore;
import com.evolution.dropfile.store.framework.KeyValueStoreInitializationGenericProcedure;
import com.evolution.dropfile.store.framework.single.SingleValueStore;
import com.evolution.dropfile.store.framework.single.SingleValueStoreInitializationGenericProcedure;
import com.evolution.dropfiledaemon.bootstrap.middleware.KeyValueStoreInitializationGenericProcedureImpl;
import com.evolution.dropfiledaemon.bootstrap.middleware.SingleValueStoreInitializationGenericProcedureImpl;
import com.evolution.dropfiledaemon.tunnel.compress.CompressTunnelService;
import com.evolution.dropfiledaemon.tunnel.compress.ZstdCompressTunnelService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.util.List;

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

    @Bean
    public CompressTunnelService compressTunnelService(DaemonApplicationProperties daemonApplicationProperties) {
        return new ZstdCompressTunnelService(daemonApplicationProperties);
    }

    @Bean
    public FileHelper fileHelper() {
        return new FileHelper();
    }

    @Bean
    public KeyValueStoreInitializationGenericProcedure keyValueStoreInitializationGenericProcedure(List<KeyValueStore> stores) {
        return new KeyValueStoreInitializationGenericProcedureImpl(stores);
    }

    @Bean
    public SingleValueStoreInitializationGenericProcedure singleValueStoreInitializationGenericProcedure(List<SingleValueStore> stores) {
        return new SingleValueStoreInitializationGenericProcedureImpl(stores);
    }
}