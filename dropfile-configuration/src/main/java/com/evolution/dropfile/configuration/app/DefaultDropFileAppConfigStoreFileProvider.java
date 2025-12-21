package com.evolution.dropfile.configuration.app;

import java.util.Optional;

public class DefaultDropFileAppConfigStoreFileProvider implements DropFileAppConfigStoreFileProvider {

    @Override
    public Optional<String> getDirectoryName() {
        return Optional.empty();
    }

    @Override
    public String getFilename() {
        return "app.config.json";
    }
}
