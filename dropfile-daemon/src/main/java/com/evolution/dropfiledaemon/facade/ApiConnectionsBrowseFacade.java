package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.ApiConnectionsBrowseGetRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsBrowseGetResponseDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsBrowseLsRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsBrowseLsResponseDTO;
import com.evolution.dropfiledaemon.download.FileDownloadOrchestrator;
import com.evolution.dropfiledaemon.download.FileDownloadRequest;
import com.evolution.dropfiledaemon.download.FileDownloadResponse;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.tunnel.command.dto.ShareLsTunnelResponse;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClientGateway;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Component
public class ApiConnectionsBrowseFacade {

    private final TunnelClientGateway tunnelClientGateway;

    private final HandshakeTrustedOutStore handshakeTrustedOutStore;

    private final FileDownloadOrchestrator fileDownloadOrchestrator;

    public List<ApiConnectionsBrowseLsResponseDTO> ls(ApiConnectionsBrowseLsRequestDTO requestDTO) {
        Map.Entry<String, HandshakeTrustedOutStore.TrustedOut> lastHandshake = handshakeTrustedOutStore.getRequiredLastUpdated();
        String fingerprintConnection = lastHandshake.getKey();
        return ls(fingerprintConnection, requestDTO);
    }

    @SneakyThrows
    private List<ApiConnectionsBrowseLsResponseDTO> ls(String fingerprint,
                                                       ApiConnectionsBrowseLsRequestDTO requestDTO) {
        List<ShareLsTunnelResponse> files = tunnelClientGateway.shareLs(fingerprint, requestDTO.ids());
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
        Map.Entry<String, HandshakeTrustedOutStore.TrustedOut> lastHandshake = handshakeTrustedOutStore.getRequiredLastUpdated();
        String fingerprintConnection = lastHandshake.getKey();

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
