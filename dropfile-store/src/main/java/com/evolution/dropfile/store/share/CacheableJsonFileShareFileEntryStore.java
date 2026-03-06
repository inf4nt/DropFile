package com.evolution.dropfile.store.share;

import com.evolution.dropfile.store.framework.CacheableKeyValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CacheableJsonFileShareFileEntryStore
        extends CacheableKeyValueStore<ShareFileEntry>
        implements ShareFileEntryStore {

    public CacheableJsonFileShareFileEntryStore(ObjectMapper objectMapper) {
        super(new JsonFileShareFileEntryStore(objectMapper));
    }
}
