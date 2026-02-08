package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.dto.ApiDownloadLsDTO;
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

    public List<ApiDownloadLsDTO.Response> ls(ApiDownloadLsDTO.Request request) {
        List<ApiDownloadLsDTO.Response> responses = new ArrayList<>();
        Map<String, DownloadFileEntry> entryMap = fileDownloadEntryStore.getAll();
        for (Map.Entry<String, DownloadFileEntry> entry : entryMap.entrySet()) {
            FileDownloadOrchestrator.DownloadProgress downloadProgress = fileDownloadOrchestrator
                    .getDownloadProcedures()
                    .get(entry.getKey());
            if (downloadProgress != null) {
                responses.add(new ApiDownloadLsDTO.Response(
                        downloadProgress.operationId(),
                        downloadProgress.fileId(),
                        entry.getValue().destinationFile(),
                        null,
                        downloadProgress.downloaded(),
                        downloadProgress.total(),
                        downloadProgress.percentage(),
                        ApiDownloadLsDTO.Status.DOWNLOADING,
                        entry.getValue().updated()
                ));
            } else {
                String operation = entry.getKey();
                DownloadFileEntry downloadFileEntry = entry.getValue();
                ApiDownloadLsDTO.Status status = ApiDownloadLsDTO.Status.valueOf(downloadFileEntry.status().name());
                responses.add(new ApiDownloadLsDTO.Response(
                        operation,
                        downloadFileEntry.fileId(),
                        downloadFileEntry.destinationFile(),
                        status == ApiDownloadLsDTO.Status.COMPLETED ? downloadFileEntry.hash() : null,
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
                addAll(getByStatus(responses, ApiDownloadLsDTO.Status.ERROR, limit));
                addAll(getByStatus(responses, ApiDownloadLsDTO.Status.STOPPED, limit));
                addAll(getByStatus(responses, ApiDownloadLsDTO.Status.COMPLETED, limit));
                addAll(getByStatus(responses, ApiDownloadLsDTO.Status.DOWNLOADING, limit));
            } else {
                ApiDownloadLsDTO.Status status = ApiDownloadLsDTO.Status.valueOf(request.status().name());
                addAll(getByStatus(responses, status, limit));
            }
        }};
    }

    private List<ApiDownloadLsDTO.Response> getByStatus(List<ApiDownloadLsDTO.Response> source, ApiDownloadLsDTO.Status status, int limit) {
        return source.stream()
                .filter(it -> it.status() == status)
                .sorted(Comparator.comparing(ApiDownloadLsDTO.Response::updated))
                .limit(limit)
                .toList();
    }

    public boolean stop(String operationId) {
        return fileDownloadOrchestrator.stop(operationId);
    }

    public void stopAll() {
        fileDownloadOrchestrator.stopAll();
    }

    public void rm(String operationId) {
        String key = fileDownloadEntryStore.getRequiredByKeyStartWith(operationId).getKey();
        fileDownloadEntryStore.remove(key);
    }

    public void rmAll() {
        fileDownloadEntryStore.removeAll();
    }
}
