package com.evolution.dropfiledaemon.files;

import com.evolution.dropfile.common.dto.DownloadFileTunnelRequest;
import com.evolution.dropfile.common.dto.DownloadFileTunnelResponse;
import com.evolution.dropfile.configuration.files.FileEntry;
import com.evolution.dropfile.configuration.files.FileEntryStore;
import com.evolution.dropfiledaemon.tunnel.framework.handler.ActionHandler;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;

@Component
public class DownloadFileActionHandler
        implements ActionHandler<DownloadFileTunnelRequest, DownloadFileTunnelResponse> {

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
    public Class<DownloadFileTunnelRequest> getPayloadType() {
        return DownloadFileTunnelRequest.class;
    }

    @SneakyThrows
    @Override
    public DownloadFileTunnelResponse handle(DownloadFileTunnelRequest requestDTO) {
        FileEntry fileEntry = fileEntryStore.get(requestDTO.id()).orElseThrow();
        byte[] allBytes = Files.readAllBytes(Paths.get(fileEntry.absolutePath()));
        return new DownloadFileTunnelResponse(fileEntry.id(), fileEntry.alias(), allBytes);
    }
}
