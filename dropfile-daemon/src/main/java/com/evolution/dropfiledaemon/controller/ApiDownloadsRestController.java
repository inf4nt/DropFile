package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.common.dto.ApiDownloadFileResponse;
import com.evolution.dropfile.store.download.DownloadFileEntry;
import com.evolution.dropfile.store.download.FileDownloadEntryStore;
import com.evolution.dropfiledaemon.file.FileDownloadOrchestrator;
import com.evolution.dropfiledaemon.util.FileHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/downloads")
@RequiredArgsConstructor
public class ApiDownloadsRestController {

    private final FileDownloadOrchestrator fileDownloadOrchestrator;

    private final FileDownloadEntryStore fileDownloadEntryStore;

    private final FileHelper fileHelper;

    @GetMapping("/ls")
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
            List<ApiDownloadFileResponse> downloading = responses.stream()
                    .filter(it -> it.status() == ApiDownloadFileResponse.Status.DOWNLOADING)
                    .sorted((o1, o2) -> o2.updated().compareTo(o1.updated()))
                    .toList();
            List<ApiDownloadFileResponse> completed = responses.stream()
                    .filter(it -> it.status() == ApiDownloadFileResponse.Status.COMPLETED)
                    .sorted((o1, o2) -> o2.updated().compareTo(o1.updated()))
                    .toList();
            List<ApiDownloadFileResponse> error = responses.stream()
                    .filter(it -> it.status() == ApiDownloadFileResponse.Status.ERROR)
                    .sorted((o1, o2) -> o2.updated().compareTo(o1.updated()))
                    .toList();
            addAll(downloading);
            addAll(completed);
            addAll(error);
        }};
    }

    @PostMapping("/stop/{operationId}")
    public ResponseEntity<Void> stop(@PathVariable String operationId) {
        throw new UnsupportedOperationException("Unsupported yet");
    }

    @DeleteMapping("/rm/{operationId}")
    public ResponseEntity<Void> rm(@PathVariable String operationId) {
        throw new UnsupportedOperationException("Unsupported yet");
    }

    @DeleteMapping("/rm-all")
    public ResponseEntity<Void> rmAll() {
        throw new UnsupportedOperationException("Unsupported yet");
    }
}
