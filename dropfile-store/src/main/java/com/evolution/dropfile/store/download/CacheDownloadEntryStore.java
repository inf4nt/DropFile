package com.evolution.dropfile.store.download;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.store.framework.file.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;

public class CacheDownloadEntryStore
        extends CacheFileKeyValueStore<DownloadFileEntry>
        implements FileDownloadEntryStore {

    public CacheDownloadEntryStore(FileHelper fileHelper,
                                   ObjectMapper objectMapper,
                                   Path parrentDirectoryPath) {
        super(
                new FileProviderImpl(parrentDirectoryPath, "download.file.entries.json"),
                new FileSystemOperations(fileHelper),
                new JsonSerdeOperations<>(objectMapper, DownloadFileEntry.class)
        );
    }
}
