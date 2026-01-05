package com.evolution.dropfile.store.files;

import com.evolution.dropfile.store.store.RuntimeKeyValueStore;

public class RuntimeFileEntryStore
        extends RuntimeKeyValueStore<String, FileEntry>
        implements FileEntryStore {
}
