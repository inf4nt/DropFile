package com.evolution.dropfilecli.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;

@Configuration
public class DropFileCliApplicationConfiguration {

    private static final String HOME_DIR = ".dropfile";

    private static final String CONFIGURATION_FILE = "config.json";

    private static final URI DAEMON_URI = URI.create("http://127.0.0.1:8080");

    @Autowired
    private Environment environment;

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
    public DropFileCliConfiguration dropFileConfiguration() {
        File systemUserHome = new File(System.getProperty("user.home"));
        File homeDir = new File(systemUserHome, HOME_DIR);
        if (!homeDir.exists()) {
            Files.createDirectories(homeDir.toPath());
        }
        File cofigurationFile = new File(homeDir, CONFIGURATION_FILE);
        if (!cofigurationFile.exists()) {
            Files.createFile(cofigurationFile.toPath());
            createDefaultConfiguration(cofigurationFile);
        }
        DropFileCliConfiguration dropFileCliConfiguration = readConfiguration(cofigurationFile);
        if (!dropFileCliConfiguration.getDownloadDirectory().exists()) {
            Files.createDirectory(dropFileCliConfiguration.getDownloadDirectory().toPath());
        }

        String environmentDaemonUri = environment.getProperty("daemon_uri");
        if (environmentDaemonUri != null) {
            dropFileCliConfiguration.setDaemonURI(URI.create(environmentDaemonUri));
        }

        return dropFileCliConfiguration;
    }

    @SneakyThrows
    private DropFileCliConfiguration readConfiguration(File configFile) {
        byte[] bytes = FileUtils.readFileToByteArray(configFile);
        return objectMapper().readValue(bytes, DropFileCliConfiguration.class);
    }

    @SneakyThrows
    private void createDefaultConfiguration(File cofigurationFile) {
        DropFileCliConfiguration dropFileCliConfiguration = new DropFileCliConfiguration(
                DAEMON_URI,
                new File(System.getProperty("user.home"), HOME_DIR)
        );
        byte[] bytes = objectMapper().writeValueAsBytes(dropFileCliConfiguration);
        FileUtils.writeByteArrayToFile(cofigurationFile, bytes);
    }
}
