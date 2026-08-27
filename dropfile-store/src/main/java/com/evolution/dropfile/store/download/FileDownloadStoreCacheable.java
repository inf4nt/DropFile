package com.evolution.dropfile.store.download;

import com.evolution.dropfile.store.framework.file.CacheableFileKeyValueStore;
import com.evolution.dropfile.store.framework.file.FileOperations;
import com.evolution.dropfile.store.framework.file.FileProvider;
import com.evolution.dropfile.store.framework.file.SerdeOperations;

public class FileDownloadStoreCacheable
        extends CacheableFileKeyValueStore<DownloadFile>
        implements FileDownloadStore {

    public FileDownloadStoreCacheable(FileProvider fileProvider,
                                      FileOperations fileOperations,
                                      SerdeOperations<DownloadFile> serdeOperations) {
        super(fileProvider, fileOperations, serdeOperations);
    }
}
