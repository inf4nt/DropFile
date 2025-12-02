package com.evolution.dropfile.configuration.secret;

import com.evolution.dropfile.configuration.AbstractProtectedConfigManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class DropFileSecretsConfigManager extends AbstractProtectedConfigManager {

    private static final String SECRETS_CONFIG_FILENAME = "secrets.config.json";

    private final ObjectMapper objectMapper;

    public DropFileSecretsConfigManager(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DropFileSecretsConfig get() {
        DropFileSecretsConfig config = read();
        if (config != null) {
            return config;
        }
        initConfigFiles();
        initDefaultConfig();
        return read();
    }

    @SneakyThrows
    public void refreshDaemonToken() {
        DropFileSecretsConfig current = read();
        if (current == null) {
            return;
        }

        String daemonToken = UUID.randomUUID().toString();
        DropFileSecretsConfig config = new DropFileSecretsConfig(daemonToken);
        Path configPath = resolveConfigPath();
        String jsonConfig = objectMapper.writeValueAsString(config);
        Files.writeString(configPath, jsonConfig);
    }

    @SneakyThrows
    public DropFileSecretsConfig read(File file) {
        if (Files.notExists(file.toPath()) || Files.size(file.toPath()) == 0) {
            return null;
        }
        return objectMapper.readValue(file, DropFileSecretsConfig.class);
    }

    @SneakyThrows
    private DropFileSecretsConfig read() {
        Path configPath = resolveConfigPath();
        return read(configPath.toFile());
    }

    @SneakyThrows
    private void initConfigFiles() {
        Path homePath = resolveHomePath();
        if (Files.notExists(homePath)) {
            Files.createDirectory(homePath);
        }
        Path configPath = resolveConfigPath();
        if (Files.notExists(configPath)) {
            Files.createFile(configPath);
        }
    }

    @SneakyThrows
    private void initDefaultConfig() {
        Path configPath = resolveConfigPath();
        String daemonToken = UUID.randomUUID().toString();
        DropFileSecretsConfig config = new DropFileSecretsConfig(daemonToken);
        String configJson = objectMapper.writeValueAsString(config);
        Files.writeString(configPath, configJson);
    }

    private Path resolveConfigPath() {
        Path homePath = resolveHomePath();
        return homePath.resolve(SECRETS_CONFIG_FILENAME);
    }
}
