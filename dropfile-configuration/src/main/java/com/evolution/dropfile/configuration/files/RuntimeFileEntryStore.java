package com.evolution.dropfile.configuration.files;

import com.evolution.dropfile.configuration.store.RuntimeKeyValueStore;

public class RuntimeFileEntryStore
        extends RuntimeKeyValueStore<String, FileEntry>
        implements FileEntryStore {
}
