package com.evolution.dropfile.store.quickshare;

import com.evolution.dropfile.store.framework.KeyValueStore;

public interface QuickShareStore extends KeyValueStore<QuickShare> {

    @Override
    default void validate(String key, QuickShare value) {
        if (value.secure() && (value.secret() == null || value.secret().isBlank())) {
            throw new IllegalArgumentException("QuickShareStore action failed. " +
                    "QuickShareEntry must have a non-empty secret: " + key);
        }
        if (!value.secure() && value.secret() != null) {
            throw new IllegalArgumentException("QuickShareStore action failed. " +
                    "QuickShareEntry insecure must have an empty secret: " + key);
        }

        get(key).ifPresentOrElse(
                entryState -> {
                    if (entryState.getValue().expired()) {
                        throw new IllegalArgumentException("QuickShareStore action failed. " +
                                "Unable to UPDATE already expired entry: " + key);
                    }
                },
                () -> {
                    if (value.expired()) {
                        throw new IllegalArgumentException("QuickShareStore action failed. " +
                                "Unable to CREATE already expired entry: " + key);
                    }
                }
        );
    }
}
