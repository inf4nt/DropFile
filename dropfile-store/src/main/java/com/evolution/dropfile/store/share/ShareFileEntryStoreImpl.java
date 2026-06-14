package com.evolution.dropfile.store.share;

import com.evolution.dropfile.store.framework.file.CacheFileKeyValueStore;
import com.evolution.dropfile.store.framework.file.FileOperations;
import com.evolution.dropfile.store.framework.file.FileProvider;
import com.evolution.dropfile.store.framework.file.SerdeOperations;

public class ShareFileEntryStoreImpl
        extends CacheFileKeyValueStore<ShareFileEntry>
        implements ShareFileEntryStore {

    public ShareFileEntryStoreImpl(FileProvider fileProvider, FileOperations fileOperations, SerdeOperations<ShareFileEntry> serdeOperations) {
        super(fileProvider, fileOperations, serdeOperations);
    }
}
