package com.evolution.dropfiledaemon.files;

import com.evolution.dropfile.store.files.FileEntry;
import com.evolution.dropfile.store.files.FileEntryStore;
import com.evolution.dropfiledaemon.tunnel.framework.ActionHandler;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Component
public class ShareCatActionHandler implements ActionHandler<String, String> {

    private final FileEntryStore fileEntryStore;

    @Autowired
    public ShareCatActionHandler(FileEntryStore fileEntryStore) {
        this.fileEntryStore = fileEntryStore;
    }

    @Override
    public String getAction() {
        return "share-cat";
    }

    @Override
    public Class<String> getPayloadType() {
        return String.class;
    }

    @SneakyThrows
    @Override
    public String handle(String id) {
        FileEntry fileEntry = fileEntryStore.get(id)
                .map(it -> it.getValue())
                .orElse(null);
        if (fileEntry == null) {
            return null;
        }
        byte[] bytes = Files.readAllBytes(Paths.get(fileEntry.absolutePath()));
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
