package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.dto.ApiConnectionsShareDownloadRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsShareDownloadResponseDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsShareLsRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsShareLsResponseDTO;
import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.download.FileDownloadOrchestrator;
import com.evolution.dropfiledaemon.download.FileDownloadRequest;
import com.evolution.dropfiledaemon.download.FileDownloadResponse;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClient;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareLsTunnelRequest;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareLsTunnelResponse;
import com.evolution.dropfiledaemon.util.FileHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Component
public class ApiConnectionsShareFacade {

    private final ApplicationConfigStore applicationConfigStore;

    private final TunnelClient tunnelClient;

    private final FileDownloadOrchestrator fileDownloadOrchestrator;

    private final FileHelper fileHelper;

    public List<ApiConnectionsShareLsResponseDTO> ls(ApiConnectionsShareLsRequestDTO requestDTO) {
        String fingerprintConnection = applicationConfigStore.getHandshakeStore()
                .sessionOutStore().getRequiredLatestUpdated()
                .getKey();
        return ls(fingerprintConnection, requestDTO);
    }

    private List<ApiConnectionsShareLsResponseDTO> ls(String fingerprint,
                                                      ApiConnectionsShareLsRequestDTO requestDTO) {
        List<ShareLsTunnelResponse> files = tunnelClient.send(
                TunnelClient.Request.builder()
                        .command("share-ls")
                        .body(new ShareLsTunnelRequest(requestDTO.ids()))
                        .fingerprint(fingerprint)
                        .build(),
                new TypeReference<List<ShareLsTunnelResponse>>() {
                }
        );
        return files.stream()
                .map(it -> new ApiConnectionsShareLsResponseDTO(
                        it.id(),
                        it.alias(),
                        fileHelper.toDisplaySize(it.size()),
                        it.created()
                ))
                .toList();
    }

    public String cat(String id) {
        return tunnelClient.send(
                TunnelClient.Request.builder()
                        .command("share-cat")
                        .body(id)
                        .fingerprint(
                                applicationConfigStore.getHandshakeStore()
                                        .sessionOutStore().getRequiredLatestUpdated()
                                        .getKey()
                        )
                        .build(),
                String.class
        );
    }

    private FileDownloadRequest getRequestForDownloadRequest(String fingerprintConnection, ApiConnectionsShareDownloadRequestDTO requestDTO) {
        List<ApiConnectionsShareLsResponseDTO> responses = ls(
                fingerprintConnection, new ApiConnectionsShareLsRequestDTO(List.of(requestDTO.fileId()))
        );
        if (responses.isEmpty()) {
            throw new RuntimeException("No files to download were found");
        }
        if (responses.size() > 1) {
            List<String> ids = responses.stream().map(it -> it.id()).toList();
            throw new RuntimeException(String.format(
                    "Multiple files %s to download were found. Specify only one file to download", ids
            ));
        }
        ApiConnectionsShareLsResponseDTO response = responses.getFirst();
        return new FileDownloadRequest(
                fingerprintConnection,
                response.id(),
                ObjectUtils.isEmpty(requestDTO.filename()) ? response.alias() : requestDTO.filename()
        );
    }

    public ApiConnectionsShareDownloadResponseDTO download(ApiConnectionsShareDownloadRequestDTO requestDTO) {
        String fingerprintConnection = applicationConfigStore.getHandshakeStore()
                .sessionOutStore().getRequiredLatestUpdated()
                .getKey();

        FileDownloadRequest fileDownloadRequest = getRequestForDownloadRequest(fingerprintConnection, requestDTO);

        // TODO remove it
        if (requestDTO.filename() != null && requestDTO.filename().contains("big")) {
            int index = requestDTO.filename().indexOf("big");
            Integer iterations = Integer.valueOf(requestDTO.filename().substring(0, index));
            for (int i = 0; i < iterations; i++) {
                String filename = i + "-" + fileDownloadRequest.filename();
                fileDownloadOrchestrator.start(
                        new FileDownloadRequest(fileDownloadRequest.fingerprintConnection(), fileDownloadRequest.fileId(), filename)
                );
            }
            return null;
        }

        FileDownloadResponse fileDownloadResponse = fileDownloadOrchestrator.start(fileDownloadRequest);

        return new ApiConnectionsShareDownloadResponseDTO(
                fileDownloadResponse.operationId(),
                fingerprintConnection,
                fileDownloadResponse.fileId(),
                fileDownloadResponse.filename()
        );
    }
}
