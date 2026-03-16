package com.evolution.dropfile.store.share;

import com.evolution.dropfile.store.framework.file.FileProviderImpl;
import com.evolution.dropfile.store.framework.file.JsonFileOperations;
import com.evolution.dropfile.store.framework.file.SynchronizedFileKeyValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;

public class JsonFileShareFileEntryStore
        extends SynchronizedFileKeyValueStore<ShareFileEntry>
        implements ShareFileEntryStore {

    public JsonFileShareFileEntryStore(ObjectMapper objectMapper, Path parrentDirectoryPath) {
        super(
                new FileProviderImpl(parrentDirectoryPath, "share.file.entries.json"),
                new JsonFileOperations<>(
                        objectMapper,
                        ShareFileEntry.class
                )
        );
    }
}
