package com.evolution.dropfile.store.download;

import com.evolution.dropfile.store.framework.KeyValueStore;

public interface FileDownloadStore
        extends KeyValueStore<DownloadFile> {

    @Override
    default void validate(String key, DownloadFile value) {
        DownloadFile downloadFile = get(key)
                .map(it -> it.getValue())
                .orElse(null);
        if (downloadFile == null) {
            return;
        }
        DownloadFile.DownloadFileEntryStatus currentStatus = downloadFile.status();
        if (!canTransitionTo(currentStatus)) {
            throw new IllegalArgumentException("FileDownloadEntryStore action failed. Status transition failed. Key %s status from %s to %s"
                    .formatted(key, currentStatus, value.status())
            );
        }
    }

    private boolean canTransitionTo(DownloadFile.DownloadFileEntryStatus current) {
        return switch (current) {
            case DOWNLOADING -> true;
            case COMPLETED, ERROR, INTERRUPTED, STOPPED -> false;
        };
    }
}
