package com.evolution.dropfile.store.download;

import com.evolution.dropfile.store.framework.file.CacheableFileKeyValueStore;
import com.evolution.dropfile.store.framework.file.FileOperations;
import com.evolution.dropfile.store.framework.file.FileProvider;
import com.evolution.dropfile.store.framework.file.SerdeOperations;

public class FileDownloadEntryStoreCacheable
        extends CacheableFileKeyValueStore<DownloadFileEntry>
        implements FileDownloadEntryStore {

    public FileDownloadEntryStoreCacheable(FileProvider fileProvider,
                                           FileOperations fileOperations,
                                           SerdeOperations<DownloadFileEntry> serdeOperations) {
        super(fileProvider, fileOperations, serdeOperations);
    }
}
