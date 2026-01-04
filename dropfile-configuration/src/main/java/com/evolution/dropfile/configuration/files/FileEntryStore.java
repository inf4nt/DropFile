package com.evolution.dropfile.configuration.files;

import com.evolution.dropfile.configuration.store.KeyValueStore;

import java.util.Map;

public interface FileEntryStore extends KeyValueStore<String, FileEntry> {

    @Override
    default void validateUpdate(String key, FileEntry value) {
        Map.Entry<String, FileEntry> alias = getAll().entrySet()
                .stream()
                .filter(it -> it.getValue().alias().equals(value.alias()))
                .findAny()
                .orElse(null);
        if (alias != null) {
            throw new RuntimeException(String.format(
                    "Duplicate file alias %s %s", value.alias(), alias.getKey()
            ));
        }

        Map.Entry<String, FileEntry> fileAbsolutePath = getAll().entrySet().stream()
                .filter(it -> it.getValue().absolutePath().equals(value.absolutePath()))
                .findAny()
                .orElse(null);
        if (fileAbsolutePath != null) {
            throw new RuntimeException(String.format(
                    "Duplicate absolute file path %s %s", value.absolutePath(), fileAbsolutePath.getKey()
            ));
        }
    }
}
