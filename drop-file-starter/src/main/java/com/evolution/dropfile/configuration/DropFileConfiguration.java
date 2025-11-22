package com.evolution.dropfile.configuration;

import lombok.SneakyThrows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Configuration
public class DropFileConfiguration {

    private static final String HOME_DIR = "DropFile";

    private static final String CONFIG_FILENAME = "config.json";

    @Bean
    @SneakyThrows
    public DropFileConfig getDropFileDaemonConfig() {
        prepareDefaultConfig();

        return new DropFileConfig();
    }

    public DropFileConfig readConfig() {
        Path configPath = resolveConfigPath();

        return null;
    }

    @SneakyThrows
    private void prepareDefaultConfig() {
        Path configPath = resolveConfigPath();
        if (Files.notExists(configPath)) {
            Files.createFile(configPath);
        }
    }

    private void writeDefaultConfig(File configFile) {
    }

    private Path resolveConfigPath() {
        String base = isWindows() ? System.getenv("LOCALAPPDATA") : System.getProperty("user.home");
        Objects.requireNonNull(base);

        return Path.of(base, HOME_DIR, CONFIG_FILENAME);
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }
}
