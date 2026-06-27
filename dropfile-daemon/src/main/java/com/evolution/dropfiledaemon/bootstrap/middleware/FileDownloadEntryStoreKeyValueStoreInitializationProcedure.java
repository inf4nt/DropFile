package com.evolution.dropfiledaemon.bootstrap.middleware;

import com.evolution.dropfile.store.download.DownloadFileEntry;
import com.evolution.dropfile.store.download.FileDownloadEntryStore;
import com.evolution.dropfile.store.framework.KeyValueStoreInitializationProcedure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class FileDownloadEntryStoreKeyValueStoreInitializationProcedure
        implements KeyValueStoreInitializationProcedure {

    private final FileDownloadEntryStore store;

    @Override
    public void init() {
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

            return staleDownloads;
        });

    }
}
