package com.evolution.dropfile.store.download;

import com.evolution.dropfile.store.framework.CacheableKeyValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CacheableJsonFileFileDownloadEntryStore
        extends CacheableKeyValueStore<DownloadFileEntry>
        implements FileDownloadEntryStore {

    public CacheableJsonFileFileDownloadEntryStore(ObjectMapper objectMapper) {
        super(new JsonFileFileDownloadEntryStore(objectMapper));
    }
}
