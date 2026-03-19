package com.evolution.dropfile.store.download;

import com.evolution.dropfile.store.framework.KeyValueStore;

public interface FileDownloadEntryStore
        extends KeyValueStore<DownloadFileEntry> {

    @Override
    default void validate(String key, DownloadFileEntry value) {
        DownloadFileEntry downloadFileEntry = get(key)
                .map(it -> it.getValue())
                .orElse(null);
        if (downloadFileEntry == null) {
            return;
        }
        DownloadFileEntry.DownloadFileEntryStatus currentStatus = downloadFileEntry.status();
        if (!canTransitionTo(currentStatus)) {
            throw new IllegalStateException(String.format("Status transition failed. Key %s status from %s to %s", key, currentStatus, value.status()));
        }
    }

    private boolean canTransitionTo(DownloadFileEntry.DownloadFileEntryStatus current) {
        return switch (current) {
            case DOWNLOADING -> true;
            case COMPLETED, ERROR, INTERRUPTED, STOPPED -> false;
        };
    }
}
