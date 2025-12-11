package com.evolution.dropfile.configuration.secret;

import com.evolution.dropfile.configuration.AbstractConfigManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class DropFileSecretsConfigManager extends AbstractConfigManager {

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
        byte[] data = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(config);
        writePath(configPath, data);
    }

    @SneakyThrows
    private DropFileSecretsConfig read() {
        Path configPath = resolveConfigPath();
        return read(configPath.toFile());
    }

    @SneakyThrows
    private DropFileSecretsConfig read(File file) {
        if (Files.notExists(file.toPath()) || Files.size(file.toPath()) == 0) {
            return null;
        }
        byte[] bytes = readPath(file.toPath());
        return objectMapper.readValue(bytes, DropFileSecretsConfig.class);
    }

    @SneakyThrows
    private void initConfigFiles() {
        Path configPath = resolveConfigPath();
        createFiles(configPath.toFile());
    }

    @SneakyThrows
    private void initDefaultConfig() {
        Path configPath = resolveConfigPath();
        String daemonToken = UUID.randomUUID().toString();
        DropFileSecretsConfig config = new DropFileSecretsConfig(daemonToken);
        byte[] data = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(config);
        writePath(configPath, data);
    }

    private Path resolveConfigPath() {
        return resolveProtectedHomeDirectory()
                .resolve(SECRETS_CONFIG_FILENAME);
    }
}
