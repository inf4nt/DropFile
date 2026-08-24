package com.evolution.dropfile.store.quickshare;

import com.evolution.dropfile.store.framework.KeyValueStore;

public interface QuickShareEntryStore extends KeyValueStore<QuickShareEntry> {

    @Override
    default void validate(String key, QuickShareEntry value) {
        if (value.secure() && (value.secret() == null || value.secret().isBlank())) {
            throw new IllegalArgumentException("QuickShareEntryStore action failed. " +
                    "QuickShareEntry must have a non-empty secret: " + key);
        }
        if (!value.secure() && value.secret() != null) {
            throw new IllegalArgumentException("QuickShareEntryStore action failed. " +
                    "QuickShareEntry insecure must have an empty secret: " + key);
        }

        get(key).ifPresentOrElse(
                entryState -> {
                    if (entryState.getValue().expired()) {
                        throw new IllegalArgumentException("QuickShareEntryStore action failed. " +
                                "Unable to UPDATE already expired entry: " + key);
                    }
                },
                () -> {
                    if (value.expired()) {
                        throw new IllegalArgumentException("QuickShareEntryStore action failed. " +
                                "Unable to CREATE already expired entry: " + key);
                    }
                }
        );
    }
}
