package com.evolution.dropfile.store.download;

import com.evolution.dropfile.store.framework.KeyValueStoreInitializationProcedure;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

// TODO move me to Daemon
public class FileDownloadEntryStoreKeyValueStoreInitializationProcedure
        implements KeyValueStoreInitializationProcedure<FileDownloadEntryStore> {

    @Override
    public void init(FileDownloadEntryStore store) {
        store.init();

        store.save(() -> {
            Map<String, DownloadFileEntry> currentValues = store.getAll();

            Map<String, DownloadFileEntry> staleDownloads = currentValues
                    .entrySet().stream()
                    .filter(it -> it.getValue().status()
                            .equals(DownloadFileEntry.DownloadFileEntryStatus.DOWNLOADING)
                    )
                    .collect(Collectors.toMap(
                            it -> it.getKey(),
                            it -> it.getValue()
                                    .withStatus(DownloadFileEntry.DownloadFileEntryStatus.INTERRUPTED)
                    ));

            if (staleDownloads.isEmpty()) {
                return Collections.emptyMap();
            }

            Map<String, DownloadFileEntry> all = new LinkedHashMap<>(currentValues);
            all.putAll(staleDownloads);
            return all;
        });
    }
}
