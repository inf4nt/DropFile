package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.common.crypto.CryptoTunnelChaCha20Poly1305;
import com.evolution.dropfile.store.framework.file.ApplicationFingerprintSupplier;
import com.evolution.dropfile.store.framework.file.ApplicationFingerprintSupplierImpl;
import com.evolution.dropfile.store.framework.file.FileProvider;
import com.evolution.dropfile.store.framework.file.FileProviderImpl;
import com.evolution.dropfiledaemon.compress.CompressTunnelService;
import com.evolution.dropfiledaemon.compress.ZstdCompressTunnelService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
    public CompressTunnelService compressTunnelService(DaemonApplicationProperties daemonApplicationProperties) {
        return new ZstdCompressTunnelService(daemonApplicationProperties);
    }

    @Bean
    public FileHelper fileHelper(DaemonApplicationProperties applicationProperties) {
        return new FileHelper(applicationProperties.fileOperationsBufferSize);
    }

    @Bean
    public ApplicationFingerprintSupplier applicationFingerprintSupplier(DaemonApplicationProperties applicationProperties) {
        FileProvider fileProvider = new FileProviderImpl(
                Paths.get(applicationProperties.configDirectory),
                ".fingerprint.bin"
        );
        return new ApplicationFingerprintSupplierImpl(fileProvider);
    }
}