package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.dto.ApiDownloadFileResponse;
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

    public List<ApiDownloadFileResponse> ls() {
        List<ApiDownloadFileResponse> responses = new ArrayList<>();
        Map<String, DownloadFileEntry> entryMap = fileDownloadEntryStore.getAll();
        for (Map.Entry<String, DownloadFileEntry> entry : entryMap.entrySet()) {
            FileDownloadOrchestrator.DownloadProgress downloadProgress = fileDownloadOrchestrator
                    .getDownloadProcedures()
                    .get(entry.getKey());
            if (downloadProgress != null) {
                responses.add(new ApiDownloadFileResponse(
                        downloadProgress.operationId(),
                        downloadProgress.fileId(),
                        entry.getValue().destinationFile(),
                        null,
                        downloadProgress.downloaded(),
                        downloadProgress.total(),
                        downloadProgress.percentage(),
                        ApiDownloadFileResponse.Status.DOWNLOADING,
                        entry.getValue().updated()
                ));
            } else {
                String operation = entry.getKey();
                DownloadFileEntry downloadFileEntry = entry.getValue();
                ApiDownloadFileResponse.Status status = ApiDownloadFileResponse.Status.valueOf(downloadFileEntry.status().name());
                responses.add(new ApiDownloadFileResponse(
                        operation,
                        downloadFileEntry.fileId(),
                        downloadFileEntry.destinationFile(),
                        status == ApiDownloadFileResponse.Status.COMPLETED ? downloadFileEntry.hash() : null,
                        downloadFileEntry.downloaded(),
                        downloadFileEntry.total(),
                        fileHelper.percent(downloadFileEntry.downloaded(), downloadFileEntry.total()),
                        status,
                        downloadFileEntry.updated()
                ));
            }
        }

        return new ArrayList<>() {{
            addAll(getByStatus(responses, ApiDownloadFileResponse.Status.ERROR));
            addAll(getByStatus(responses, ApiDownloadFileResponse.Status.STOPPED));
            addAll(getByStatus(responses, ApiDownloadFileResponse.Status.COMPLETED));
            addAll(getByStatus(responses, ApiDownloadFileResponse.Status.DOWNLOADING));
        }};
    }

    private List<ApiDownloadFileResponse> getByStatus(List<ApiDownloadFileResponse> source, ApiDownloadFileResponse.Status status) {
        return source.stream()
                .filter(it -> it.status() == status)
                .sorted(Comparator.comparing(ApiDownloadFileResponse::updated))
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
