package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.dto.ApiConnectionsShareDownloadRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsShareDownloadResponseDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsShareLsResponseDTO;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareLsTunnelResponse;
import com.evolution.dropfiledaemon.download.FileDownloadOrchestrator;
import com.evolution.dropfiledaemon.download.FileDownloadRequest;
import com.evolution.dropfiledaemon.download.FileDownloadResponse;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClient;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ApiConnectionsShareFacade {

    private final TunnelClient tunnelClient;

    private final FileDownloadOrchestrator fileDownloadOrchestrator;

    @Autowired
    public ApiConnectionsShareFacade(TunnelClient tunnelClient,
                                     FileDownloadOrchestrator fileDownloadOrchestrator) {
        this.tunnelClient = tunnelClient;
        this.fileDownloadOrchestrator = fileDownloadOrchestrator;
    }

    public List<ApiConnectionsShareLsResponseDTO> ls() {
        List<ShareLsTunnelResponse> files = tunnelClient.send(
                TunnelClient.Request.builder()
                        .command("share-ls")
                        .build(),
                new TypeReference<List<ShareLsTunnelResponse>>() {
                }
        );
        return files.stream()
                .map(it -> new ApiConnectionsShareLsResponseDTO(it.id(), it.alias()))
                .toList();
    }

    public String cat(String id) {
        return tunnelClient.send(
                TunnelClient.Request.builder()
                        .command("share-cat")
                        .body(id)
                        .build(),
                String.class
        );
    }

    public ApiConnectionsShareDownloadResponseDTO download(ApiConnectionsShareDownloadRequestDTO requestDTO) {
        if (requestDTO.filename() != null && requestDTO.filename().contains("big")) {
            int index = requestDTO.filename().indexOf("big");
            Integer iterations = Integer.valueOf(requestDTO.filename().substring(0, index));
            for (int i = 0; i < iterations; i++) {
                fileDownloadOrchestrator.start(
                        new FileDownloadRequest(requestDTO.id(), i + "-" + requestDTO.filename())
                );
            }
            return null;
        }

        FileDownloadResponse fileDownloadResponse = fileDownloadOrchestrator.start(
                new FileDownloadRequest(requestDTO.id(), requestDTO.filename())
        );
        return new ApiConnectionsShareDownloadResponseDTO(
                fileDownloadResponse.operationId(),
                fileDownloadResponse.filename()
        );
    }
}
