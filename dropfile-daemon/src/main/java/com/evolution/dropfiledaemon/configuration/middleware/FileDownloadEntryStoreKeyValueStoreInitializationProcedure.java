package com.evolution.dropfiledaemon.configuration.middleware;

import com.evolution.dropfile.store.download.DownloadFileEntry;
import com.evolution.dropfile.store.download.FileDownloadEntryStore;
import com.evolution.dropfile.store.framework.KeyValueStoreInitializationProcedure;import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
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

            log.info("Found {} invalid file download entries {}", staleDownloads.size(), staleDownloads.keySet());

            Map<String, DownloadFileEntry> all = new LinkedHashMap<>(currentValues);
            all.putAll(staleDownloads);
            return all;
        });

    }
}
