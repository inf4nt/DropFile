package com.evolution.dropfile.store.quickshare;

import com.evolution.dropfile.store.framework.KeyValueStore;

public interface QuickShareEntryStore extends KeyValueStore<QuickShareEntry> {

    @Override
    default void validate(String key, QuickShareEntry value) {
        // TODO add validation
//        Map.Entry<String, QuickShareEntry> entryState = get(key).orElse(null);
//        if (entryState == null) {
//            if (value.used()) {
//                throw new RuntimeException("'Used' field for new entries must be set as 'false': " + key);
//            }
//        }
//        if (entryState != null) {
//            if (!entryState.getValue().used() && value.used()) {
//                return;
//            }
//            throw new RuntimeException("Update unsupported: " + key);
//        }
    }
}
