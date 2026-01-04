package com.evolution.dropfiledaemon.files;

import com.evolution.dropfile.common.dto.DownloadFileRequestDTO;
import com.evolution.dropfile.common.dto.DownloadFileResponseDTO;
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
        implements ActionHandler<DownloadFileRequestDTO, DownloadFileResponseDTO> {

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
    public Class<DownloadFileRequestDTO> getPayloadType() {
        return DownloadFileRequestDTO.class;
    }

    @SneakyThrows
    @Override
    public DownloadFileResponseDTO handle(DownloadFileRequestDTO requestDTO) {
        FileEntry fileEntry = fileEntryStore.get(requestDTO.id()).orElseThrow();
        byte[] allBytes = Files.readAllBytes(Paths.get(fileEntry.absolutePath()));
        return new DownloadFileResponseDTO(fileEntry.id(), fileEntry.alias(), allBytes);
    }
}
