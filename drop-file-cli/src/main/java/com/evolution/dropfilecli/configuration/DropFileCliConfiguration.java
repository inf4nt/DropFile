package com.evolution.dropfilecli.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;

@Configuration
public class DropFileCliConfiguration {

    private static final String HOME_DIR = ".dropfile";

    private static final String CONFIGURATION_FILE = "config.json";

    private static final URI DAEMON_URI = URI.create("http://127.0.0.1:8080");

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder().build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @SneakyThrows
    @Bean
    public DropFileConfiguration dropFileConfiguration() {
        File systemUserHome = new File(System.getProperty("user.home"));
        File homeDir = new File(systemUserHome, HOME_DIR);
        if (!homeDir.exists()) {
            homeDir.mkdir();
        }
        File cofigurationFile = new File(homeDir, CONFIGURATION_FILE);
        if (!cofigurationFile.exists()) {
            cofigurationFile.createNewFile();
            createDefaultConfiguration(cofigurationFile);
        }
        DropFileConfiguration dropFileConfiguration = readConfiguration(cofigurationFile);
        if (!dropFileConfiguration.getDownloadDirectory().exists()) {
            dropFileConfiguration.getDownloadDirectory().mkdir();
        }
        System.out.println("DropFileConfiguration: " + dropFileConfiguration);

        return dropFileConfiguration;
    }

    @SneakyThrows
    private DropFileConfiguration readConfiguration(File configFile) {
        byte[] bytes = FileUtils.readFileToByteArray(configFile);
        return objectMapper().readValue(bytes, DropFileConfiguration.class);
    }

    @SneakyThrows
    private void createDefaultConfiguration(File cofigurationFile) {
        DropFileConfiguration dropFileConfiguration = new DropFileConfiguration(
                DAEMON_URI,
                new File(System.getProperty("user.home"), HOME_DIR)
        );
        byte[] bytes = objectMapper().writeValueAsBytes(dropFileConfiguration);
        FileUtils.writeByteArrayToFile(cofigurationFile, bytes);
    }
}
