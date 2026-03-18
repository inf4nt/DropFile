package com.evolution.dropfile.store.download;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.store.framework.CacheableKeyValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;

public class CacheableJsonFileFileDownloadEntryStore
        extends CacheableKeyValueStore<DownloadFileEntry>
        implements FileDownloadEntryStore {

    public CacheableJsonFileFileDownloadEntryStore(FileHelper fileHelper, ObjectMapper objectMapper, Path parrentDirectoryPath) {
        super(new JsonFileFileDownloadEntryStore(fileHelper, objectMapper, parrentDirectoryPath));
    }
}
