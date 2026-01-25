package com.evolution.dropfile.store.download;

import com.evolution.dropfile.store.store.json.DefaultJsonSerde;
import com.evolution.dropfile.store.store.json.FileProvider;
import com.evolution.dropfile.store.store.json.JsonFileKeyValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.nio.file.Paths;

public class JsonFileFileDownloadEntryStore
        extends JsonFileKeyValueStore<DownloadFileEntry>
        implements FileDownloadEntryStore {
    public JsonFileFileDownloadEntryStore(ObjectMapper objectMapper) {
        super(
                new FileProvider() {
                    @Override
                    public Path getFilePath() {
                        return Paths.get("download.file.entries.json");
                    }
                },
                new DefaultJsonSerde<>(
                        DownloadFileEntry.class,
                        objectMapper
                )
        );
    }
}
