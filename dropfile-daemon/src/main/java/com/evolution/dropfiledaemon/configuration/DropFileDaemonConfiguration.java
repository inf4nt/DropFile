package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.common.crypto.CryptoTunnelChaCha20Poly1305;
import com.evolution.dropfile.store.framework.file.DirectoryProvider;
import com.evolution.dropfile.store.framework.file.DirectoryProviderImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.nio.file.Paths;

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
    public FileHelper fileHelper() {
        return new FileHelper();
    }

    @Bean
    public DirectoryProvider applicationDirectory(@Value("${user.dir}") String rootDirectory) {
        return new DirectoryProviderImpl(Paths.get(rootDirectory));
    }

    @Bean
    public DirectoryProvider downloadDirectory(@Value("${dropfile.download.directory}") String downloadDirectory) {
        return new DirectoryProviderImpl(Paths.get(downloadDirectory));
    }
}