package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.ApiConnectionsBrowseGetRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsBrowseGetResponseDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsBrowseLsRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsBrowseLsResponseDTO;
import com.evolution.dropfiledaemon.download.FileDownloadOrchestrator;
import com.evolution.dropfiledaemon.download.FileDownloadRequest;
import com.evolution.dropfiledaemon.download.FileDownloadResponse;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionOutStore;
import com.evolution.dropfiledaemon.tunnel.command.dto.ShareLsTunnelRequest;
import com.evolution.dropfiledaemon.tunnel.command.dto.ShareLsTunnelResponse;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClient;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Component
public class ApiConnectionsBrowseFacade {

    private final TunnelClient tunnelClient;

    private final FileDownloadOrchestrator fileDownloadOrchestrator;

    private final HandshakeSessionOutStore handshakeSessionOutStore;

    public List<ApiConnectionsBrowseLsResponseDTO> ls(ApiConnectionsBrowseLsRequestDTO requestDTO) {
        String fingerprintConnection = handshakeSessionOutStore.getRequiredLatestUpdated()
                .getKey();
        return ls(fingerprintConnection, requestDTO);
    }

    private List<ApiConnectionsBrowseLsResponseDTO> ls(String fingerprint,
                                                       ApiConnectionsBrowseLsRequestDTO requestDTO) {
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
                .map(it -> new ApiConnectionsBrowseLsResponseDTO(
                        it.id(),
                        it.alias(),
                        CommonUtils.toDisplaySize(it.size()),
                        it.created()
                ))
                .toList();
    }

    private FileDownloadRequest getRequestForDownloadRequest(String fingerprintConnection, ApiConnectionsBrowseGetRequestDTO requestDTO) {
        List<ApiConnectionsBrowseLsResponseDTO> responses = ls(
                fingerprintConnection, new ApiConnectionsBrowseLsRequestDTO(List.of(requestDTO.fileId()))
        );
        ApiConnectionsBrowseLsResponseDTO response = CommonUtils.requireOne(responses);
        return new FileDownloadRequest(
                fingerprintConnection,
                response.id(),
                ObjectUtils.isEmpty(requestDTO.filename()) ? response.alias() : requestDTO.filename()
        );
    }

    public ApiConnectionsBrowseGetResponseDTO get(ApiConnectionsBrowseGetRequestDTO requestDTO) {
        String fingerprintConnection = handshakeSessionOutStore.getRequiredLatestUpdated()
                .getKey();

        FileDownloadRequest fileDownloadRequest = getRequestForDownloadRequest(fingerprintConnection, requestDTO);

        // TODO remove it
        if (requestDTO.filename() != null && requestDTO.filename().contains("big")) {
            int index = requestDTO.filename().indexOf("big");
            Integer iterations = Integer.valueOf(requestDTO.filename().substring(0, index));
            for (int i = 0; i < iterations; i++) {
                String filename = i + "-" + fileDownloadRequest.filename();
                fileDownloadOrchestrator.start(
                        new FileDownloadRequest(fileDownloadRequest.fingerprint(), fileDownloadRequest.fileId(), filename)
                );
            }
            return null;
        }

        FileDownloadResponse fileDownloadResponse = fileDownloadOrchestrator.start(fileDownloadRequest);

        return new ApiConnectionsBrowseGetResponseDTO(
                fileDownloadResponse.operationId(),
                fingerprintConnection,
                fileDownloadResponse.fileId(),
                fileDownloadResponse.filename()
        );
    }
}
