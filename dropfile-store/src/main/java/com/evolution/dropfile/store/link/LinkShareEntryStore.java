package com.evolution.dropfile.store.link;

import com.evolution.dropfile.store.framework.KeyValueStore;

import java.util.Map;

public interface LinkShareEntryStore extends KeyValueStore<LinkShareEntry> {

    @Override
    default void validate(String key, LinkShareEntry value) {
        Map.Entry<String, LinkShareEntry> entryState = get(key).orElse(null);
        if (entryState == null) {
            if (value.used()) {
                throw new RuntimeException("'Used' field for new entries must be set as 'false': " + key);
            }
        }
        if (entryState != null) {
            if (!entryState.getValue().used() && value.used()) {
                return;
            }
            throw new RuntimeException("Update unsupported: " + key);
        }
    }
}
