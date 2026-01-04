package com.evolution.dropfile.configuration.files;

import com.evolution.dropfile.configuration.store.KeyValueStore;

import java.util.Map;

public interface FileEntryStore extends KeyValueStore<String, FileEntry> {

    @Override
    default void validate(String key, FileEntry value) {
        Map.Entry<String, FileEntry> alias = getAll().entrySet()
                .stream()
                .filter(it -> it.getValue().alias().equals(value.alias()))
                .filter(it -> !it.getKey().equals(key))
                .findAny()
                .orElse(null);
        if (alias != null) {
            throw new RuntimeException(String.format(
                    "Duplicate file alias %s %s", value.alias(), alias.getKey()
            ));
        }

        Map.Entry<String, FileEntry> fileAbsolutePath = getAll().entrySet().stream()
                .filter(it -> it.getValue().absolutePath().equals(value.absolutePath()))
                .filter(it -> !it.getKey().equals(key))
                .findAny()
                .orElse(null);
        if (fileAbsolutePath != null) {
            throw new RuntimeException(String.format(
                    "Duplicate absolute file path %s %s", value.absolutePath(), fileAbsolutePath.getKey()
            ));
        }
    }
}
