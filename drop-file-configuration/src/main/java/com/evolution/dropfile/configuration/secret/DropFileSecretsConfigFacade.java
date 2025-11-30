package com.evolution.dropfile.configuration.secret;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class DropFileSecretsConfigFacade {

    private static final String SECRETS_WINDOWS_HOME_DIR = "DropFile";

    private static final String SECRETS_UNIX_HOME_DIR = ".dropfile";

    private static final String SECRETS_CONFIG_FILENAME = "secrets.config.json";

    private static final DropFileSecretsConfig EMPTY = new DropFileSecretsConfig();

    private final ObjectMapper objectMapper;

    public DropFileSecretsConfigFacade(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DropFileSecretsConfig get() {
        DropFileSecretsConfig config = read();
        if (config == null) {
            initConfigFiles();
            initDefaultConfig();
        }
        return read();
    }

    @SneakyThrows
    public void refreshDaemonToken() {
        String daemonToken = UUID.randomUUID().toString();
        DropFileSecretsConfig config = new DropFileSecretsConfig(daemonToken);
        Path configPath = resolveConfigPath();
        String jsonConfig = objectMapper.writeValueAsString(config);
        Files.writeString(configPath, jsonConfig);
    }

    @SneakyThrows
    private DropFileSecretsConfig read() {
        Path configPath = resolveConfigPath();
        if (Files.notExists(configPath) || Files.size(configPath) == 0) {
            return null;
        }
        return objectMapper.readValue(configPath.toFile(), DropFileSecretsConfig.class);
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

    private Path resolveHomePath() {
        String basePath;
        String dirName;
        if (isWindows()) {
            basePath = System.getenv("LOCALAPPDATA");
            dirName = SECRETS_WINDOWS_HOME_DIR;
        } else if (isLinux() || isMacOs()) {
            basePath = System.getProperty("user.home");
            dirName = SECRETS_UNIX_HOME_DIR;
        } else {
            throw new UnsupportedOperationException(
                    "Unsupported operating system " + getOs()
            );
        }
        return Path.of(basePath, dirName);
    }

    private boolean isMacOs() {
        return getOs().toLowerCase().startsWith("mac");
    }

    private boolean isLinux() {
        return getOs().toLowerCase().startsWith("linux");
    }

    private boolean isWindows() {
        return getOs().toLowerCase().startsWith("windows");
    }

    private String getOs() {
        return System.getProperty("os.name");
    }
}
