package com.evolution.dropfile.store.share;

import com.evolution.dropfile.store.framework.file.FileProviderImpl;
import com.evolution.dropfile.store.framework.file.JsonFileOperations;
import com.evolution.dropfile.store.framework.file.SynchronizedFileKeyValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonFileShareFileEntryStore
        extends SynchronizedFileKeyValueStore<ShareFileEntry>
        implements ShareFileEntryStore {

    public JsonFileShareFileEntryStore(ObjectMapper objectMapper) {
        super(
                new FileProviderImpl("share.file.entries.json"),
                new JsonFileOperations<>(
                        objectMapper,
                        ShareFileEntry.class
                )
        );
    }
}
