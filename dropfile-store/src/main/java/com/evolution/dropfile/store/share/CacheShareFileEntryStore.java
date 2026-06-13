package com.evolution.dropfile.store.share;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.store.framework.file.CacheFileKeyValueStore;
import com.evolution.dropfile.store.framework.file.FileProviderImpl;
import com.evolution.dropfile.store.framework.file.FileSystemOperations;
import com.evolution.dropfile.store.framework.file.JsonSerdeOperations;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;

public class CacheShareFileEntryStore
        extends CacheFileKeyValueStore<ShareFileEntry>
        implements ShareFileEntryStore {

    public CacheShareFileEntryStore(FileHelper fileHelper,
                                    ObjectMapper objectMapper,
                                    Path parrentDirectoryPath) {
        super(
                new FileProviderImpl(parrentDirectoryPath, "share.file.entries.json"),
                new FileSystemOperations(
                        fileHelper
                ),
                new JsonSerdeOperations<>(
                        objectMapper,
                        ShareFileEntry.class
                )
        );
    }
}
