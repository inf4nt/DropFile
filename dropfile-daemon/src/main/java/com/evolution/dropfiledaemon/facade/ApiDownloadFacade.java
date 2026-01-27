package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.dto.ApiDownloadLsRequest;
import com.evolution.dropfile.common.dto.ApiDownloadLsResponse;
import com.evolution.dropfile.store.download.DownloadFileEntry;
import com.evolution.dropfile.store.download.FileDownloadEntryStore;
import com.evolution.dropfiledaemon.download.FileDownloadOrchestrator;
import com.evolution.dropfiledaemon.util.FileHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class ApiDownloadFacade {

    private final FileDownloadOrchestrator fileDownloadOrchestrator;

    private final FileDownloadEntryStore fileDownloadEntryStore;

    private final FileHelper fileHelper;

    public List<ApiDownloadLsResponse> ls(ApiDownloadLsRequest request) {
        List<ApiDownloadLsResponse> responses = new ArrayList<>();
        Map<String, DownloadFileEntry> entryMap = fileDownloadEntryStore.getAll();
        for (Map.Entry<String, DownloadFileEntry> entry : entryMap.entrySet()) {
            FileDownloadOrchestrator.DownloadProgress downloadProgress = fileDownloadOrchestrator
                    .getDownloadProcedures()
                    .get(entry.getKey());
            if (downloadProgress != null) {
                responses.add(new ApiDownloadLsResponse(
                        downloadProgress.operationId(),
                        downloadProgress.fileId(),
                        entry.getValue().destinationFile(),
                        null,
                        downloadProgress.downloaded(),
                        downloadProgress.total(),
                        downloadProgress.percentage(),
                        ApiDownloadLsResponse.Status.DOWNLOADING,
                        entry.getValue().updated()
                ));
            } else {
                String operation = entry.getKey();
                DownloadFileEntry downloadFileEntry = entry.getValue();
                ApiDownloadLsResponse.Status status = ApiDownloadLsResponse.Status.valueOf(downloadFileEntry.status().name());
                responses.add(new ApiDownloadLsResponse(
                        operation,
                        downloadFileEntry.fileId(),
                        downloadFileEntry.destinationFile(),
                        status == ApiDownloadLsResponse.Status.COMPLETED ? downloadFileEntry.hash() : null,
                        downloadFileEntry.downloaded(),
                        downloadFileEntry.total(),
                        fileHelper.percent(downloadFileEntry.downloaded(), downloadFileEntry.total()),
                        status,
                        downloadFileEntry.updated()
                ));
            }
        }

        return new ArrayList<>() {{
            int limit = request.limit() == null ? Integer.MAX_VALUE : request.limit();
            if (request.status() == null) {
                addAll(getByStatus(responses, ApiDownloadLsResponse.Status.ERROR, limit));
                addAll(getByStatus(responses, ApiDownloadLsResponse.Status.STOPPED, limit));
                addAll(getByStatus(responses, ApiDownloadLsResponse.Status.COMPLETED, limit));
                addAll(getByStatus(responses, ApiDownloadLsResponse.Status.DOWNLOADING, limit));
            } else {
                ApiDownloadLsResponse.Status status = ApiDownloadLsResponse.Status.valueOf(request.status().name());
                addAll(getByStatus(responses, status, limit));
            }
        }};
    }

    private List<ApiDownloadLsResponse> getByStatus(List<ApiDownloadLsResponse> source, ApiDownloadLsResponse.Status status, int limit) {
        return source.stream()
                .filter(it -> it.status() == status)
                .sorted(Comparator.comparing(ApiDownloadLsResponse::updated))
                .limit(limit)
                .toList();
    }

    public boolean stop(String operationId) {
        return fileDownloadOrchestrator.stop(operationId);
    }

    public void stopAll() {
        fileDownloadOrchestrator.stopAll();
    }

    public boolean rm(String operationId) {
        return fileDownloadEntryStore.remove(operationId) != null;
    }

    public void rmAll() {
        fileDownloadEntryStore.removeAll();
    }
}
