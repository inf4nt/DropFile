package com.evolution.dropfiledaemon.controller.api;

import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfile.common.dto.ApiBatchOperationResult;
import com.evolution.dropfile.common.dto.ApiDownloadLsDTO;
import com.evolution.dropfile.common.dto.ApiDownloadRmRequest;
import com.evolution.dropfile.common.dto.ApiDownloadRmResponse;
import com.evolution.dropfiledaemon.facade.ApiDownloadFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/connections/download")
public class ApiConnectionsDownloadRestController {

    private final ApiDownloadFacade downloadFacade;

    @PostMapping("/ls")
    public List<ApiDownloadLsDTO.Response> ls(@RequestBody ApiDownloadLsDTO.Request request) {
        return downloadFacade.ls(request);
    }

    @PostMapping("/stop")
    public ApiBatchOperationResult stop(@RequestBody Collection<CriteriaEnvelope> operationIdCriteriaEnvelopes) {
        return downloadFacade.stop(operationIdCriteriaEnvelopes);
    }

    @DeleteMapping("/rm")
    public ApiDownloadRmResponse rm(@RequestBody ApiDownloadRmRequest request) {
        return downloadFacade.rm(request);
    }

    @PostMapping("/kill")
    public ApiBatchOperationResult kill(@RequestBody Collection<CriteriaEnvelope> operationIdCriteriaEnvelopes) {
        return downloadFacade.kill(operationIdCriteriaEnvelopes);
    }

    @PostMapping("/stop-all")
    public void stopAll() {
        downloadFacade.stopAll();
    }

    @DeleteMapping("/rm-all")
    public void rmAll() {
        downloadFacade.rmAll();
    }

    @PostMapping("/kill-all")
    public void killAll() {
        downloadFacade.killAll();
    }
}
