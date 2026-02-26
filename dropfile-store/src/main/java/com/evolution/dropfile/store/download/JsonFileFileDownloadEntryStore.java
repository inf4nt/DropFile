package com.evolution.dropfile.store.download;

import com.evolution.dropfile.store.framework.file.FileProviderImpl;
import com.evolution.dropfile.store.framework.file.JsonFileOperations;
import com.evolution.dropfile.store.framework.file.SynchronizedFileKeyValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonFileFileDownloadEntryStore
        extends SynchronizedFileKeyValueStore<DownloadFileEntry>
        implements FileDownloadEntryStore {

    public JsonFileFileDownloadEntryStore(ObjectMapper objectMapper) {
        super(
                new FileProviderImpl("download.file.entries.json"),
                new JsonFileOperations<>(
                        objectMapper,
                        DownloadFileEntry.class
                )
        );
    }
}
