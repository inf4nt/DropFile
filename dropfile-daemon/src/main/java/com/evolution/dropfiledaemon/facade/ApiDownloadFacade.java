package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfile.common.dto.ApiBatchOperationResult;
import com.evolution.dropfile.common.dto.ApiDownloadLsDTO;
import com.evolution.dropfile.store.download.DownloadFile;
import com.evolution.dropfile.store.download.FileDownloadStore;
import com.evolution.dropfiledaemon.download.FileDownloadOrchestrator;
import com.evolution.dropfiledaemon.download.FileDownloadOrchestrator.DownloadProgress;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@RequiredArgsConstructor
@Component
public class ApiDownloadFacade {

    private final FileDownloadOrchestrator fileDownloadOrchestrator;

    private final FileDownloadStore fileDownloadStore;

    public List<ApiDownloadLsDTO.Response> ls(ApiDownloadLsDTO.Request request) {
        Map<String, DownloadProgress> activeProcedures = fileDownloadOrchestrator.getDownloadProcedures();

        List<ApiDownloadLsDTO.Response> responses = fileDownloadStore.getAll().entrySet().stream()
                .map(entry -> toResponse(entry.getKey(), entry.getValue(), activeProcedures.get(entry.getKey())))
                .toList();

        ApiDownloadLsDTO.Status targetStatus = request.status() != null
                ? ApiDownloadLsDTO.Status.valueOf(request.status().name())
                : null;

        return filterAndLimit(responses, targetStatus, request.limit());
    }

    public ApiBatchOperationResult kill(Set<CriteriaEnvelope> operationIdCriteriaEnvelopes) {
        FileDownloadOrchestrator.FileDownloadOrchestratorKillResponse response = fileDownloadOrchestrator.kill(operationIdCriteriaEnvelopes);
        return ApiBatchOperationResult.of(response.found(), response.notFound(), response.ambiguous());
    }

    public void killAll() {
        fileDownloadOrchestrator.killAll();
    }

    private ApiDownloadLsDTO.Response toResponse(
            String operationId,
            DownloadFile downloadFile,
            @Nullable DownloadProgress progress) {

        long total = progress != null ? progress.total() : downloadFile.total();
        long downloaded = progress != null ? progress.downloaded() : downloadFile.downloaded();

        String progressDisplay = formatProgress(total, downloaded);
        String speedDisplay = progress != null ? CommonUtils.toDisplaySize(progress.speedBytesPerSec()) : null;
        boolean accessible = isAccessible(downloadFile.destinationFile());
        ApiDownloadLsDTO.Status status = ApiDownloadLsDTO.Status.valueOf(downloadFile.status().name());

        return new ApiDownloadLsDTO.Response(
                operationId,
                downloadFile.fingerprint(),
                downloadFile.fileId(),
                downloadFile.destinationFile(),
                progressDisplay,
                speedDisplay,
                accessible,
                status,
                downloadFile.created(),
                downloadFile.updated()
        );
    }

    private List<ApiDownloadLsDTO.Response> filterAndLimit(
            List<ApiDownloadLsDTO.Response> responses,
            @Nullable ApiDownloadLsDTO.Status targetStatus,
            @Nullable Integer rawLimit) {

        int limit = (rawLimit == null || rawLimit <= 0) ? Integer.MAX_VALUE : rawLimit;

        if (targetStatus != null) {
            return responses.stream()
                    .filter(r -> r.status() == targetStatus)
                    .limit(limit)
                    .toList();
        }

        return Arrays.stream(ApiDownloadLsDTO.Status.values())
                .flatMap(status -> responses.stream()
                        .filter(r -> r.status() == status)
                        .limit(limit))
                .toList();
    }

    private String formatProgress(long total, long downloaded) {
        if (total == 0) {
            return "0%";
        }
        if (downloaded == 0) {
            return "%s/0 (0%%)".formatted(CommonUtils.toDisplaySize(total));
        }
        if (total == downloaded) {
            return "%s (100%%)".formatted(CommonUtils.toDisplaySize(total));
        }
        return "%s/%s (%s)".formatted(
                CommonUtils.toDisplaySize(total),
                CommonUtils.toDisplaySize(downloaded),
                CommonUtils.percent(total, downloaded)
        );
    }

    private boolean isAccessible(String destinationFile) {
        return Files.exists(Path.of(destinationFile));
    }
}
