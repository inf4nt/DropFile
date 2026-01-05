package com.evolution.dropfile.store.files;

import com.evolution.dropfile.store.store.KeyValueStore;

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
    }
}
