package com.evolution.dropfiledaemon.controller.api;

import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfile.common.dto.ApiBatchOperationResult;
import com.evolution.dropfile.common.dto.ApiDownloadLsDTO;
import com.evolution.dropfiledaemon.facade.ApiDownloadFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/kill")
    public ApiBatchOperationResult kill(@RequestBody Collection<CriteriaEnvelope> operationIdCriteriaEnvelopes) {
        return downloadFacade.kill(operationIdCriteriaEnvelopes);
    }

    @PostMapping("/kill-all")
    public void killAll() {
        downloadFacade.killAll();
    }
}
