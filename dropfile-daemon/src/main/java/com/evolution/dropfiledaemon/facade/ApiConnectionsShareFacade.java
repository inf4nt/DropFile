package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.dto.*;
import com.evolution.dropfile.store.app.AppConfigStore;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClient;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

@Component
public class ApiConnectionsShareFacade {

    private final TunnelClient tunnelClient;

    private final AppConfigStore appConfigStore;

    @Autowired
    public ApiConnectionsShareFacade(TunnelClient tunnelClient,
                                     AppConfigStore appConfigStore) {
        this.tunnelClient = tunnelClient;
        this.appConfigStore = appConfigStore;
    }

    public List<ApiConnectionsShareLsResponseDTO> ls() {
        List<FileEntryTunnelResponse> files = tunnelClient.send(
                TunnelClient.Request.builder()
                        .action("share-ls")
                        .build(),
                new TypeReference<List<FileEntryTunnelResponse>>() {
                }
        );
        if (files == null) {
            return Collections.emptyList();
        }
        return files.stream()
                .map(it -> new ApiConnectionsShareLsResponseDTO(it.id(), it.alias()))
                .toList();
    }

    @SneakyThrows
    public ApiConnectionsShareDownloadResponseDTO download(ApiConnectionsShareDownloadRequestDTO requestDTO) {
        DownloadFileTunnelResponse responseDTO = tunnelClient.send(
                TunnelClient.Request.builder()
                        .action("share-download")
                        .body(requestDTO.id())
                        .build(),
                DownloadFileTunnelResponse.class
        );
        if (responseDTO == null) {
            return null;
        }

        String downloadDirectory = appConfigStore.getRequired().daemonAppConfig().downloadDirectory();
        File downloadFile;
        if (ObjectUtils.isEmpty(requestDTO.filename())) {
            downloadFile = new File(new File(downloadDirectory), responseDTO.id() + "-" + responseDTO.alias());
        } else {
            downloadFile = new File(new File(downloadDirectory), requestDTO.filename());
        }

        if (Files.notExists(downloadFile.toPath())) {
            Files.createFile(downloadFile.toPath());
        }

        if (!requestDTO.rewrite() && Files.size(downloadFile.toPath()) != 0) {
            throw new RuntimeException("Unable to rewrite file");
        }

        Files.write(downloadFile.toPath(), responseDTO.payload());
        return new ApiConnectionsShareDownloadResponseDTO(downloadFile.getAbsolutePath());
    }

    public String cat(String id) {
        return tunnelClient.send(
                TunnelClient.Request.builder()
                        .action("share-cat")
                        .body(id)
                        .build(),
                String.class
        );
    }
}
