package com.evolution.dropfile.store.share;

import com.evolution.dropfile.store.framework.CacheableKeyValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;

public class CacheableJsonFileShareFileEntryStore
        extends CacheableKeyValueStore<ShareFileEntry>
        implements ShareFileEntryStore {

    public CacheableJsonFileShareFileEntryStore(ObjectMapper objectMapper, Path parrentDirectoryPath) {
        super(new JsonFileShareFileEntryStore(objectMapper, parrentDirectoryPath));
    }
}
