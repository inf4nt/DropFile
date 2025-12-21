package com.evolution.dropfile.configuration.keys;

import java.util.Optional;

public class DefaultDropFileKeysConfigFileProvider implements DropFileKeysConfigFileProvider {
    @Override
    public Optional<String> getDirectoryName() {
        return Optional.empty();
    }

    @Override
    public String getFilename() {
        return "keys.config.json";
    }
}
