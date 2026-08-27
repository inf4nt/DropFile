package com.evolution.dropfile.store.share;

import com.evolution.dropfile.store.framework.file.CacheableFileKeyValueStore;
import com.evolution.dropfile.store.framework.file.FileOperations;
import com.evolution.dropfile.store.framework.file.FileProvider;
import com.evolution.dropfile.store.framework.file.SerdeOperations;

public class ShareFileStoreCacheable
        extends CacheableFileKeyValueStore<ShareFile>
        implements ShareFileStore {

    public ShareFileStoreCacheable(FileProvider fileProvider, FileOperations fileOperations, SerdeOperations<ShareFile> serdeOperations) {
        super(fileProvider, fileOperations, serdeOperations);
    }
}
