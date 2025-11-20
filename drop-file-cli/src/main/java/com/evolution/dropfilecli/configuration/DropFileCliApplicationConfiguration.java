package com.evolution.dropfilecli.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.net.http.HttpClient;
import java.nio.file.Files;

@Configuration
public class DropFileCliApplicationConfiguration {

    private static final String HOME_DIR = ".dropfile";

    private static final String DOWNLOAD_DIR = ".dropfile";

    private static final String CONFIG_FILENAME = "config.json";

    private static final String DAEMON_ADDRESS = "127.0.0.1:8081";

    @Value("${config.path:#{null}}")
    private String customConfigAbsoluteFilePath;

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder().build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public DropFileCliConfiguration getDropFileCliConfiguration() {
        if (!ObjectUtils.isEmpty(customConfigAbsoluteFilePath)) {
            return readConfigFile(customConfigAbsoluteFilePath);
        }

        File configFile = getOrCreateDropFileCliConfigurationFile();
        if (FileUtils.sizeOf(configFile) == 0) {
            populateConfigValuesToFile(configFile);
        }
        return readConfigFile(configFile.getAbsolutePath());
    }

    @SneakyThrows
    private DropFileCliConfiguration readConfigFile(String configFilePath) {
        File file = new File(configFilePath);
        return objectMapper().readValue(file, DropFileCliConfiguration.class);
    }

    @SneakyThrows
    private File getOrCreateDropFileCliConfigurationFile() {
        File systemHomeDirFile = new File(System.getProperty("user.home"));

        File homeDirFile = new File(systemHomeDirFile, HOME_DIR);
        if (!homeDirFile.exists()) {
            Files.createDirectories(homeDirFile.toPath());
        }

        File cliConfigFile = new File(homeDirFile, CONFIG_FILENAME);
        if (!cliConfigFile.exists()) {
            Files.createFile(cliConfigFile.toPath());
        }

        return cliConfigFile;
    }

    @SneakyThrows
    private void populateConfigValuesToFile(File cliConfigFile) {
        File systemHomeDirFile = new File(System.getProperty("user.home"));
        File defaultDownloadDirectory = new File(systemHomeDirFile, DOWNLOAD_DIR);
        if (!defaultDownloadDirectory.exists()) {
            Files.createDirectories(defaultDownloadDirectory.toPath());
        }

        DropFileCliConfiguration configFile = new DropFileCliConfiguration(
                DAEMON_ADDRESS,
                defaultDownloadDirectory
        );

        byte[] bytes = objectMapper().writeValueAsBytes(configFile);
        FileUtils.writeByteArrayToFile(cliConfigFile, bytes);
    }
}
