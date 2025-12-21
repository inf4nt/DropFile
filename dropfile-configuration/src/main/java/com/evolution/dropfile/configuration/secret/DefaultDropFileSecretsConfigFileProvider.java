package com.evolution.dropfile.configuration.secret;

import java.util.Optional;

public class DefaultDropFileSecretsConfigFileProvider
        implements DropFileSecretsConfigFileProvider {
    @Override
    public Optional<String> getDirectoryName() {
        return Optional.empty();
    }

    @Override
    public String getFilename() {
        return "secrets.config.json";
    }
}
