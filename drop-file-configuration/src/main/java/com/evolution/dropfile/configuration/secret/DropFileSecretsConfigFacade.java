package com.evolution.dropfile.configuration.secret;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.lang3.SystemProperties;
import org.apache.commons.lang3.SystemUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class DropFileSecretsConfigFacade {

    private static final String SECRETS_WINDOWS_HOME_DIR = "DropFile";

    private static final String SECRETS_UNIX_HOME_DIR = ".dropfile";

    private static final String SECRETS_CONFIG_FILENAME = "secrets.config.json";

    private final ObjectMapper objectMapper;

    public DropFileSecretsConfigFacade(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DropFileSecretsConfig get() {
        initDefaultConfig();
        DropFileSecretsConfig config = read();
        if (config == null) {
            initDefaultConfig();
        } else {
            refreshDaemonToken();
        }
        return read();
    }

    @SneakyThrows
    public DropFileSecretsConfig read() {
        Path configPath = resolveConfigPath();
        if (Files.notExists(configPath)) {
            return null;
        }
        return objectMapper.readValue(configPath.toFile(), DropFileSecretsConfig.class);
    }

    @SneakyThrows
    private void initiateConfigFiles() {
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

    @SneakyThrows
    private void refreshDaemonToken() {
        String daemonToken = UUID.randomUUID().toString();
        DropFileSecretsConfig config = new DropFileSecretsConfig(daemonToken);
        Path configPath = resolveConfigPath();
        String jsonConfig = objectMapper.writeValueAsString(config);
        Files.writeString(configPath, jsonConfig);
    }

    private Path resolveConfigPath() {
        Path homePath = resolveHomePath();
        return homePath.resolve(SECRETS_CONFIG_FILENAME);
    }

    private Path resolveHomePath() {
        String basePath;
        String dirName;
        if (SystemUtils.IS_OS_WINDOWS) {
            basePath = System.getenv("LOCALAPPDATA");
            dirName = SECRETS_WINDOWS_HOME_DIR;
        } else if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC) {
            basePath = System.getProperty("user.home");
            dirName = SECRETS_UNIX_HOME_DIR;
        } else {
            throw new UnsupportedOperationException(
                    "Unsupported operating system " + SystemProperties.getOsName()
            );
        }
        return Path.of(basePath, dirName);
    }
}
