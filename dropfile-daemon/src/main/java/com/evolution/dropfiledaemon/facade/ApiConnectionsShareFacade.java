package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.dto.ApiConnectionsShareDownloadRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsShareDownloadResponseDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsShareLsResponseDTO;
import com.evolution.dropfiledaemon.download.FileDownloadOrchestrator;
import com.evolution.dropfiledaemon.download.FileDownloadRequest;
import com.evolution.dropfiledaemon.download.FileDownloadResponse;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStore;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClient;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareLsTunnelResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Component
public class ApiConnectionsShareFacade {

    private final HandshakeStore handshakeStore;

    private final TunnelClient tunnelClient;

    private final FileDownloadOrchestrator fileDownloadOrchestrator;

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
        String fingerprintConnection = handshakeStore.trustedOutStore().getRequiredLatestUpdated().getKey();

        // TODO remove it
        if (requestDTO.filename() != null && requestDTO.filename().contains("big")) {
            int index = requestDTO.filename().indexOf("big");
            Integer iterations = Integer.valueOf(requestDTO.filename().substring(0, index));
            for (int i = 0; i < iterations; i++) {
                fileDownloadOrchestrator.start(
                        new FileDownloadRequest(fingerprintConnection,
                                requestDTO.fileId(), i + "-" + requestDTO.filename())
                );
            }
            return null;
        }

        FileDownloadResponse fileDownloadResponse = fileDownloadOrchestrator.start(
                new FileDownloadRequest(fingerprintConnection, requestDTO.fileId(), requestDTO.filename())
        );
        return new ApiConnectionsShareDownloadResponseDTO(
                fileDownloadResponse.operationId(),
                fingerprintConnection,
                requestDTO.fileId(),
                fileDownloadResponse.filename()
        );
    }
}
