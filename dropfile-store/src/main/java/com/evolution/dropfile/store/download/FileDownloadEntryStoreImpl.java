package com.evolution.dropfile.store.download;

import com.evolution.dropfile.store.framework.file.CacheFileKeyValueStore;
import com.evolution.dropfile.store.framework.file.FileOperations;
import com.evolution.dropfile.store.framework.file.FileProvider;
import com.evolution.dropfile.store.framework.file.SerdeOperations;

public class FileDownloadEntryStoreImpl
        extends CacheFileKeyValueStore<DownloadFileEntry>
        implements FileDownloadEntryStore {

    public FileDownloadEntryStoreImpl(FileProvider fileProvider,
                                      FileOperations fileOperations,
                                      SerdeOperations<DownloadFileEntry> serdeOperations) {
        super(fileProvider, fileOperations, serdeOperations);
    }
}
