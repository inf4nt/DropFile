package com.evolution.dropfiledaemon.controller.api;

import com.evolution.dropfile.common.dto.*;
import com.evolution.dropfiledaemon.facade.ApiDownloadFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

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
    public ApiBatchOperationResult stop(@RequestBody Set<String> startWithOperationIds) {
        return downloadFacade.stop(startWithOperationIds);
    }

    @DeleteMapping("/rm")
    public ApiDownloadRmResponse rm(@RequestBody ApiDownloadRmRequest request) {
        return downloadFacade.rm(request);
    }

    @PostMapping("/stop-all")
    public void stopAll() {
        downloadFacade.stopAll();
    }

    @DeleteMapping("/rm-all")
    public void rmAll() {
        downloadFacade.rmAll();
    }
}
