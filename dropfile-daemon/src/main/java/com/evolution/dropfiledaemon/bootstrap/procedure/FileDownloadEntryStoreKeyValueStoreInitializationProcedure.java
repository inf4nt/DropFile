package com.evolution.dropfiledaemon.bootstrap.procedure;

import com.evolution.dropfile.store.download.DownloadFile;
import com.evolution.dropfile.store.download.FileDownloadStore;
import com.evolution.dropfile.store.framework.KeyValueStoreInitializationProcedure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

// TODO Can be dropped. Think over it
@Component
@RequiredArgsConstructor
@Slf4j
public class FileDownloadEntryStoreKeyValueStoreInitializationProcedure
        implements KeyValueStoreInitializationProcedure {

    private final FileDownloadStore store;

    @Override
    public void init() {
        store.save(() -> {
            Map<String, DownloadFile> currentValues = store.getAll();

            Map<String, DownloadFile> staleDownloads = currentValues
                    .entrySet().stream()
                    .filter(it -> it.getValue().status()
                            .equals(DownloadFile.DownloadFileEntryStatus.DOWNLOADING)
                    )
                    .collect(Collectors.toMap(
                            it -> it.getKey(),
                            it -> it.getValue()
                                    .withStatus(DownloadFile.DownloadFileEntryStatus.INTERRUPTED)
                    ));

            if (staleDownloads.isEmpty()) {
                return Collections.emptyMap();
            }

            log.info("Found {} invalid file download entries {}", staleDownloads.size(), staleDownloads.keySet());

            return staleDownloads;
        });

    }
}
