package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.ApiDownloadLsDTO;
import com.evolution.dropfile.store.download.DownloadFileEntry;
import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.download.FileDownloadOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Component
public class ApiDownloadFacade {

    private final FileDownloadOrchestrator fileDownloadOrchestrator;

    private final ApplicationConfigStore applicationConfigStore;

    public List<ApiDownloadLsDTO.Response> ls(ApiDownloadLsDTO.Request request) {
        Map<String, ApiDownloadLsDTO.Response> responseMap = new LinkedHashMap<>();

        Map<String, FileDownloadOrchestrator.DownloadProgress> downloadProcedures = fileDownloadOrchestrator.getDownloadProcedures();
        Map<String, FileDownloadOrchestrator.DownloadProgress> waitingQueue = fileDownloadOrchestrator.getWaitingQueue();
        Map<String, DownloadFileEntry> getFileDownloadEntryStoreMap = applicationConfigStore.getFileDownloadEntryStore().getAll();

        for (Map.Entry<String, DownloadFileEntry> entry : getFileDownloadEntryStoreMap.entrySet()) {
            String operationId = entry.getKey();
            DownloadFileEntry downloadFileEntry = entry.getValue();

            ApiDownloadLsDTO.Status status = ApiDownloadLsDTO.Status.valueOf(downloadFileEntry.status().name());

            String progress = Optional.ofNullable(downloadProcedures.get(operationId))
                    .map(it -> getProgress(it.total(), it.downloaded()))
                    .orElse(getProgress(downloadFileEntry.total(), downloadFileEntry.downloaded()));
            String speedPerSecond = Optional.ofNullable(downloadProcedures.get(operationId))
                    .map(it -> CommonUtils.toDisplaySize(it.speedBytesPerSec()))
                    .orElse(null);

            responseMap.put(operationId, new ApiDownloadLsDTO.Response(
                    operationId,
                    downloadFileEntry.fingerprint(),
                    downloadFileEntry.fileId(),
                    downloadFileEntry.destinationFile(),
                    progress,
                    speedPerSecond,
                    status,
                    downloadFileEntry.created(),
                    downloadFileEntry.updated()
            ));
        }

        for (Map.Entry<String, FileDownloadOrchestrator.DownloadProgress> entry : waitingQueue.entrySet()) {
            String operationId = entry.getKey();
            if (responseMap.containsKey(operationId)) {
                continue;
            }

            FileDownloadOrchestrator.DownloadProgress downloadProgress = entry.getValue();

            responseMap.put(operationId, new ApiDownloadLsDTO.Response(
                    operationId,
                    downloadProgress.fingerprint(),
                    downloadProgress.fileId(),
                    downloadProgress.filename(),
                    null,
                    null,
                    ApiDownloadLsDTO.Status.QUEUE,
                    null,
                    null
            ));
        }

        List<ApiDownloadLsDTO.Response> responses = responseMap.values().stream()
                .toList();

        int limit = (request.limit() == null || request.limit() <= 0) ? Integer.MAX_VALUE : request.limit();
        if (request.status() == null) {
            return Arrays.stream(ApiDownloadLsDTO.Status.values())
                    .map(status -> getByStatus(responses, status, limit))
                    .flatMap(it -> it.stream())
                    .toList();
        }
        ApiDownloadLsDTO.Status status = ApiDownloadLsDTO.Status.valueOf(request.status().name());
        return getByStatus(responses, status, limit);
    }

    public void stop(String operationId) {
        fileDownloadOrchestrator.stop(operationId);
    }

    public void stopAll() {
        fileDownloadOrchestrator.stopAll();
    }

    public void rm(String operationId) {
        CommonUtils.executeSafety(() -> stop(operationId));
        String key = applicationConfigStore.getFileDownloadEntryStore().getRequiredByKeyStartWith(operationId).getKey();
        applicationConfigStore.getFileDownloadEntryStore().remove(key);
    }

    public void rmAll() {
        stopAll();
        applicationConfigStore.getFileDownloadEntryStore().removeAll();
    }

    private String getProgress(long total, long downloaded) {
        if (total == 0) {
            return "0%";
        }
        if (downloaded == 0) {
            return String.format("%s/0 (%s)", CommonUtils.toDisplaySize(total), "0%");
        }
        if (total == downloaded) {
            return String.format("%s (%s)", CommonUtils.toDisplaySize(total), "100%");
        }
        return String.format("%s/%s (%s)", CommonUtils.toDisplaySize(total), CommonUtils.toDisplaySize(downloaded), CommonUtils.percent(total, downloaded));
    }

    private List<ApiDownloadLsDTO.Response> getByStatus(List<ApiDownloadLsDTO.Response> source, ApiDownloadLsDTO.Status status, int limit) {
        Stream<ApiDownloadLsDTO.Response> responseStream = source.stream()
                .filter(it -> it.status() == status)
                .limit(limit);
        if (status != ApiDownloadLsDTO.Status.QUEUE) {
            responseStream = responseStream.
                    sorted(Comparator.comparing(ApiDownloadLsDTO.Response::updated).reversed())
                    .limit(limit)
                    .sorted(Comparator.comparing(ApiDownloadLsDTO.Response::updated));
        }
        return responseStream
                .toList();
    }
}
