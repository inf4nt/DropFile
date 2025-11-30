package com.evolution.dropfile.configuration.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DropFileAppConfigFacade {

    private static final String APP_CONFIG_HOME_DIR = ".dropfile";

    private static final String APP_CONFIG_FILENAME = "app.config.json";

    private static final String APP_CONFIG_DOWNLOAD_DIR = ".dropfile";

    private static final String APP_CONFIG_DAEMON_ADDRESS = "127.0.0.1:18181";

    private final ObjectMapper objectMapper;

    public DropFileAppConfigFacade(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DropFileAppConfig get() {
        DropFileAppConfig config = read();
        if (config == null) {
            initAppConfigFiles();
            initDefaultConfig();
        }
        config = read();
        return config;
    }

    @SneakyThrows
    private DropFileAppConfig read() {
        Path configPath = getAppConfigPath();
        if (Files.notExists(configPath) || Files.size(configPath) == 0) {
            return null;
        }
        return objectMapper.readValue(configPath.toFile(), DropFileAppConfig.class);
    }

    @SneakyThrows
    private void initAppConfigFiles() {
        Path appConfigHomeDirPath = getAppConfigHomeDirPath();
        if (Files.notExists(appConfigHomeDirPath)) {
            Files.createDirectory(appConfigHomeDirPath);
        }
        Path appConfigPath = getAppConfigPath();
        if (Files.notExists(appConfigPath)) {
            Files.createFile(appConfigPath);
        }
    }

    @SneakyThrows
    private void initDefaultConfig() {
        Path appConfigPath = getAppConfigPath();
        DropFileAppConfig config = new DropFileAppConfig(
                APP_CONFIG_DOWNLOAD_DIR,
                APP_CONFIG_DAEMON_ADDRESS
        );
        String jsonConfig = objectMapper.writeValueAsString(config);
        Files.writeString(appConfigPath, jsonConfig);
    }

    private Path getAppConfigHomeDirPath() {
        String userHomeBase = System.getProperty("user.home");
        return Paths.get(userHomeBase, APP_CONFIG_HOME_DIR);
    }

    private Path getAppConfigPath() {
        return getAppConfigHomeDirPath().resolve(APP_CONFIG_FILENAME);
    }
}
