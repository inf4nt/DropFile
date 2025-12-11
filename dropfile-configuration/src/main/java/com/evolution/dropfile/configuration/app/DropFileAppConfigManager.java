package com.evolution.dropfile.configuration.app;

import com.evolution.dropfile.configuration.AbstractConfigManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class DropFileAppConfigManager
        extends AbstractConfigManager {

    private static final String APP_CONFIG_FILENAME = "app.config.json";

    private static final String APP_CONFIG_DOWNLOAD_DIR = ".dropfile";

    private static final String APP_CONFIG_DAEMON_HOST = "127.0.0.1";

    private static final Integer APP_CONFIG_DAEMON_PORT = 18181;

    private final ObjectMapper objectMapper;

    public DropFileAppConfigManager(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DropFileAppConfig get() {
        DropFileAppConfig config = read();
        if (config != null) {
            return config;
        }
        initAppConfigFiles();
        initDefaultConfig();
        return read();
    }

    @SneakyThrows
    public DropFileAppConfig save(DropFileAppConfig config) {
        initAppConfigFiles();
        Path appConfigPath = resolveAppConfigPath();
        byte[] data = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsBytes(config);
        writePath(appConfigPath, data);
        return config;
    }

    @SneakyThrows
    public DropFileAppConfig read(File file) {
        if (Files.notExists(file.toPath()) || Files.size(file.toPath()) == 0) {
            return null;
        }
        byte[] data = readPath(file.toPath());
        return objectMapper.readValue(data, DropFileAppConfig.class);
    }

    @SneakyThrows
    private DropFileAppConfig read() {
        Path configPath = resolveAppConfigPath();
        return read(configPath.toFile());
    }

    @SneakyThrows
    private void initAppConfigFiles() {
        Path appConfigPath = resolveAppConfigPath();
        createFiles(appConfigPath.toFile());
    }

    @SneakyThrows
    private void initDefaultConfig() {
        DropFileAppConfig config = new DropFileAppConfig(
                new DropFileAppConfig.DropFileCliAppConfig(
                        APP_CONFIG_DAEMON_HOST,
                        APP_CONFIG_DAEMON_PORT
                ),
                new DropFileAppConfig.DropFileDaemonAppConfig(
                        APP_CONFIG_DOWNLOAD_DIR,
                        APP_CONFIG_DAEMON_PORT,
                        null
                )
        );
        save(config);
    }

    private Path resolveAppConfigPath() {
        return resolveHomeDirectory()
                .resolve(APP_CONFIG_FILENAME);
    }
}
