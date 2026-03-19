package com.evolution.dropfile.store.download;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.store.framework.file.CacheableSynchronizedFileKeyValueStore;
import com.evolution.dropfile.store.framework.file.FileProviderImpl;
import com.evolution.dropfile.store.framework.file.JsonFileOperations;
import com.evolution.dropfile.store.framework.file.SynchronizedFileKeyValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;

public class JsonFileFileDownloadEntryStore
        extends CacheableSynchronizedFileKeyValueStore<DownloadFileEntry>
        implements FileDownloadEntryStore {

    public JsonFileFileDownloadEntryStore(FileHelper fileHelper, ObjectMapper objectMapper, Path parrentDirectoryPath) {
        super(
                new FileProviderImpl(parrentDirectoryPath, "download.file.entries.json"),
                new JsonFileOperations<>(
                        fileHelper,
                        objectMapper,
                        DownloadFileEntry.class
                )
        );
    }
}
