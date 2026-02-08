package com.evolution.dropfile.store.share;

import com.evolution.dropfile.store.store.KeyValueStore;

import java.util.Map;

public interface ShareFileEntryStore extends KeyValueStore<ShareFileEntry> {

    @Override
    default void validate(String key, ShareFileEntry value) {
        Map.Entry<String, ShareFileEntry> alias = getAll().entrySet()
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
