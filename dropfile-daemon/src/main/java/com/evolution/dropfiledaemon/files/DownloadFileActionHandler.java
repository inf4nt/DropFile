package com.evolution.dropfiledaemon.files;

import com.evolution.dropfile.common.dto.DownloadFileTunnelResponse;
import com.evolution.dropfile.store.files.FileEntry;
import com.evolution.dropfile.store.files.FileEntryStore;
import com.evolution.dropfiledaemon.tunnel.framework.ActionHandler;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@Component
public class DownloadFileActionHandler
        implements ActionHandler<String, DownloadFileTunnelResponse> {

    private final FileEntryStore fileEntryStore;

    @Autowired
    public DownloadFileActionHandler(FileEntryStore fileEntryStore) {
        this.fileEntryStore = fileEntryStore;
    }

    @Override
    public String getAction() {
        return "download-file";
    }

    @Override
    public Class<String> getPayloadType() {
        return String.class;
    }

    @SneakyThrows
    @Override
    public DownloadFileTunnelResponse handle(String id) {
        Map.Entry<String, FileEntry> fileEntry = fileEntryStore.get(id).orElseThrow();
        byte[] allBytes = Files.readAllBytes(Paths.get(fileEntry.getValue().absolutePath()));
        return new DownloadFileTunnelResponse(fileEntry.getKey(), fileEntry.getValue().alias(), allBytes);
    }
}
