package com.evolution.dropfile.store.download;

import com.evolution.dropfile.store.store.RuntimeKeyValueStore;

public class RuntimeDownloadFileEntryStore
        extends RuntimeKeyValueStore<String, DownloadFileEntry>
        implements DownloadFileEntryStore {
}
