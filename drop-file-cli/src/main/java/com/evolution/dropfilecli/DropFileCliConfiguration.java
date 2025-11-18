package com.evolution.dropfilecli;

import lombok.SneakyThrows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;

@Configuration
public class DropFileCliConfiguration {

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder().build();
    }

    @Bean
    @SneakyThrows
    public DropFileProperties sessionProperties() {
        DropFileProperties dropFileProperties = new DropFileProperties(
                URI.create("http://127.0.0.1:8080"),
                new File(System.getProperty("user.home"), ".dropfile"),
                new File(System.getProperty("user.home"), ".dropfile")
        );
        if (!dropFileProperties.getHomeDownloadDirectory().exists()) {
            dropFileProperties.getHomeDownloadDirectory().mkdir();
        }
        if (!dropFileProperties.getHomeConfigurationDirectory().exists()) {
            dropFileProperties.getHomeConfigurationDirectory().mkdir();
        }
        return dropFileProperties;
    }
}
