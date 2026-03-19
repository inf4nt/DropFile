package com.evolution.dropfile.store.share;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.store.framework.file.CacheableSynchronizedFileKeyValueStore;
import com.evolution.dropfile.store.framework.file.FileProviderImpl;
import com.evolution.dropfile.store.framework.file.JsonFileOperations;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;

public class JsonFileShareFileEntryStore
        extends CacheableSynchronizedFileKeyValueStore<ShareFileEntry>
        implements ShareFileEntryStore {

    public JsonFileShareFileEntryStore(FileHelper fileHelper, ObjectMapper objectMapper, Path parrentDirectoryPath) {
        super(
                new FileProviderImpl(parrentDirectoryPath, "share.file.entries.json"),
                new JsonFileOperations<>(
                        fileHelper,
                        objectMapper,
                        ShareFileEntry.class
                )
        );
    }
}
