package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.dto.ApiDownloadLsDTO;
import com.evolution.dropfile.store.download.DownloadFileEntry;
import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.download.FileDownloadOrchestrator;
import com.evolution.dropfiledaemon.util.FileHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@RequiredArgsConstructor
@Component
public class ApiDownloadFacade {

    private final FileDownloadOrchestrator fileDownloadOrchestrator;

    private final ApplicationConfigStore applicationConfigStore;

    private final FileHelper fileHelper;

    public List<ApiDownloadLsDTO.Response> ls(ApiDownloadLsDTO.Request request) {
        List<ApiDownloadLsDTO.Response> responses = new ArrayList<>();
        Map<String, DownloadFileEntry> entryMap = applicationConfigStore.getFileDownloadEntryStore().getAll();
        for (Map.Entry<String, DownloadFileEntry> entry : entryMap.entrySet()) {
            FileDownloadOrchestrator.DownloadProgress downloadProgress = fileDownloadOrchestrator
                    .getDownloadProcedures()
                    .get(entry.getKey());
            if (downloadProgress != null) {
                ApiDownloadLsDTO.Status status = ApiDownloadLsDTO.Status.DOWNLOADING;
                String progress = getProgress(downloadProgress.total(), downloadProgress.downloaded());
                String speedPerSecond = fileHelper.toDisplaySize(downloadProgress.speedBytesPerSec());
                responses.add(new ApiDownloadLsDTO.Response(
                        downloadProgress.operationId(),
                        downloadProgress.fileId(),
                        entry.getValue().destinationFile(),
                        progress,
                        speedPerSecond,
                        status,
                        entry.getValue().updated()
                ));
            } else {
                String operation = entry.getKey();
                DownloadFileEntry downloadFileEntry = entry.getValue();
                ApiDownloadLsDTO.Status status = ApiDownloadLsDTO.Status.valueOf(downloadFileEntry.status().name());
                String progress = getProgress(downloadFileEntry.total(), downloadFileEntry.downloaded());
                responses.add(new ApiDownloadLsDTO.Response(
                        operation,
                        downloadFileEntry.fileId(),
                        downloadFileEntry.destinationFile(),
                        progress,
                        null,
                        status,
                        downloadFileEntry.updated()
                ));
            }
        }

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

    private String getProgress(long total, long downloaded) {
        if (total == 0) {
            return "0%";
        }
        if (downloaded == 0) {
            return String.format("%s/0 (%s)", fileHelper.toDisplaySize(total), "0%");
        }
        if (total == downloaded) {
            return String.format("%s (%s)", fileHelper.toDisplaySize(total), "100%");
        }
        return String.format("%s/%s (%s)", fileHelper.toDisplaySize(total), fileHelper.toDisplaySize(downloaded), fileHelper.percent(total, downloaded));
    }

    private List<ApiDownloadLsDTO.Response> getByStatus(List<ApiDownloadLsDTO.Response> source, ApiDownloadLsDTO.Status status, int limit) {
        return source.stream()
                .filter(it -> it.status() == status)
                .sorted(Comparator.comparing(ApiDownloadLsDTO.Response::updated).reversed())
                .limit(limit)
                .sorted(Comparator.comparing(ApiDownloadLsDTO.Response::updated))
                .toList();
    }

    // TODO add some Util class to do "one or nothing"
    public void stop(String operationId) {
        List<String> list = fileDownloadOrchestrator.getDownloadProcedures().keySet()
                .stream()
                .filter(it -> it.startsWith(operationId))
                .toList();
        if (list.isEmpty()) {
            throw new RuntimeException("No operation found id: " + operationId);
        }
        if (list.size() > 1) {
            throw new RuntimeException(String.format(
                    "More than one item was found. Please provide more detailed criteria. Found: %s items", list.size()
            ));
        }
        String id = list.getFirst();
        fileDownloadOrchestrator.stop(id);
    }

    public void stopAll() {
        fileDownloadOrchestrator.stopAll();
    }

    public void rm(String operationId) {
        String key = applicationConfigStore.getFileDownloadEntryStore().getRequiredByKeyStartWith(operationId).getKey();
        applicationConfigStore.getFileDownloadEntryStore().remove(key);
    }

    public void rmAll() {
        applicationConfigStore.getFileDownloadEntryStore().removeAll();
    }
}
