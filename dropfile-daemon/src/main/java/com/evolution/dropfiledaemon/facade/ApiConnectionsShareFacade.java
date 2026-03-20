package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.ApiConnectionsShareDownloadRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsShareDownloadResponseDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsShareLsRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsShareLsResponseDTO;
import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.download.FileDownloadOrchestrator;
import com.evolution.dropfiledaemon.download.FileDownloadRequest;
import com.evolution.dropfiledaemon.download.FileDownloadResponseEnvelope;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClient;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareLsTunnelRequest;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareLsTunnelResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@RequiredArgsConstructor
@Slf4j
@Component
public class ApiConnectionsShareFacade {

    private final ApplicationConfigStore applicationConfigStore;

    private final TunnelClient tunnelClient;

    private final FileDownloadOrchestrator fileDownloadOrchestrator;

    public List<ApiConnectionsShareLsResponseDTO> ls(ApiConnectionsShareLsRequestDTO requestDTO) {
        String fingerprintConnection = applicationConfigStore.getHandshakeSessionOutStore().getRequiredLatestUpdated()
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
                        CommonUtils.toDisplaySize(it.size()),
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
                                applicationConfigStore.getHandshakeSessionOutStore().getRequiredLatestUpdated()
                                        .getKey()
                        )
                        .build(),
                String.class
        );
    }

    private List<FileDownloadRequest> getRequestsForDownloadRequest(String fingerprintConnection, ApiConnectionsShareDownloadRequestDTO requestDTO) {
        List<String> requestFileIds = requestDTO.downloadItems().stream().map(it -> it.id()).toList();
        List<ApiConnectionsShareLsResponseDTO> responses = ls(
                fingerprintConnection, new ApiConnectionsShareLsRequestDTO(requestFileIds)
        );
        if (responses.isEmpty()) {
            throw new RuntimeException("No files to download were found");
        }
        List<? extends Map.Entry<String, List<String>>> errorMatches = requestFileIds.stream()
                .map(requestFileId -> {
                    List<String> matchesIds = responses.stream()
                            .map(it -> it.id())
                            .filter(id -> id.startsWith(requestFileId))
                            .toList();
                    return new AbstractMap.SimpleEntry<>(requestFileId, matchesIds);
                })
                .filter(it -> it.getValue().size() != 1)
                .toList();
        if (!errorMatches.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("\n");
            for (Map.Entry<String, List<String>> errorMatch : errorMatches) {
                stringBuilder.append("Request id = ").append(errorMatch.getKey());
                stringBuilder.append("\n");
                if (errorMatch.getValue().isEmpty()) {
                    stringBuilder.append("FileIds = no found");
                } else {
                    stringBuilder.append("FileIds more than one = ").append(errorMatch.getValue());
                }
            }
            throw new RuntimeException("Multiple files to download were found. Specify accurate criteria" + stringBuilder);
        }
        List<FileDownloadRequest> requests = requestDTO.downloadItems().stream()
                .map(downloadItem -> {
                    ApiConnectionsShareLsResponseDTO lsResponseDTO = responses.stream()
                            .filter(lsResponse -> lsResponse.id().startsWith(downloadItem.id()))
                            .toList()
                            .getFirst();
                    String filename = downloadItem.filename() == null ? lsResponseDTO.alias() : downloadItem.filename();
                    return new FileDownloadRequest(fingerprintConnection, lsResponseDTO.id(), filename);
                })
                .toList();
        return requests;
    }

    public ApiConnectionsShareDownloadResponseDTO download(ApiConnectionsShareDownloadRequestDTO requestDTO) {
        String fingerprintConnection = applicationConfigStore.getHandshakeSessionOutStore().getRequiredLatestUpdated()
                .getKey();

        List<FileDownloadRequest> requests = getRequestsForDownloadRequest(fingerprintConnection, requestDTO);

        // TODO remove it
        if (requests.size() == 1 && requests.getFirst().filename() != null) {
            String filenameRequest = requestDTO.downloadItems().getFirst().filename();
            if (filenameRequest.contains("big")) {
                int index = filenameRequest.indexOf("big");
                Integer iterations = Integer.valueOf(filenameRequest.substring(0, index));
                List<FileDownloadRequest> requestsTo = IntStream.range(0, iterations)
                        .mapToObj(number -> {
                            String filename = number + "-" + filenameRequest;
                            FileDownloadRequest request = requests.getFirst();
                            return new FileDownloadRequest(request.fingerprint(), request.fileId(), filename);
                        }).toList();
                FileDownloadResponseEnvelope envelope = fileDownloadOrchestrator.start(requestsTo, requestDTO.force());
                return new ApiConnectionsShareDownloadResponseDTO(
                        envelope.responses().stream().map(it -> new ApiConnectionsShareDownloadResponseDTO.Ok(
                                it.operationId(), it.fingerprint(), it.fileId(), it.filename()
                        )).toList(),
                        envelope.skipped().stream().map(it -> new ApiConnectionsShareDownloadResponseDTO.Skipped(
                                it.fingerprint(), it.fileId(), it.filename()
                        )).toList()
                );
            }
        }

        FileDownloadResponseEnvelope envelope = fileDownloadOrchestrator.start(requests, requestDTO.force());

        return new ApiConnectionsShareDownloadResponseDTO(
                envelope.responses().stream().map(it -> new ApiConnectionsShareDownloadResponseDTO.Ok(
                        it.operationId(), it.fingerprint(), it.fileId(), it.filename()
                )).toList(),
                envelope.skipped().stream().map(it -> new ApiConnectionsShareDownloadResponseDTO.Skipped(
                        it.fingerprint(), it.fileId(), it.filename()
                )).toList()
        );
    }
}
