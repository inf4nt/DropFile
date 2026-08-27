package com.evolution.dropfile.store.share;

import com.evolution.dropfile.store.framework.file.CacheableFileKeyValueStore;
import com.evolution.dropfile.store.framework.file.FileOperations;
import com.evolution.dropfile.store.framework.file.FileProvider;
import com.evolution.dropfile.store.framework.file.SerdeOperations;

public class ShareFileEntryStoreCacheable
        extends CacheableFileKeyValueStore<ShareFileEntry>
        implements ShareFileEntryStore {

    public ShareFileEntryStoreCacheable(FileProvider fileProvider, FileOperations fileOperations, SerdeOperations<ShareFileEntry> serdeOperations) {
        super(fileProvider, fileOperations, serdeOperations);
    }
}
