package com.evolution.dropfile.store.share;

import com.evolution.dropfile.store.framework.KeyValueStore;

import java.util.Map;

public interface ShareFileStore extends KeyValueStore<ShareFile> {

    @Override
    default void validate(String key, ShareFile value) {
        Map.Entry<String, ShareFile> alias = getAll().entrySet()
                .stream()
                .filter(it -> it.getValue().alias().equals(value.alias()))
                .filter(it -> !it.getKey().equals(key))
                .findAny()
                .orElse(null);
        if (alias != null) {
            throw new IllegalArgumentException(String.format(
                    "Duplicate file alias %s %s", value.alias(), alias.getKey()
            ));
        }
    }
}
