package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.common.dto.ApiDownloadLsDTO;
import com.evolution.dropfiledaemon.facade.ApiDownloadFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/download")
public class ApiDownloadRestController {

    private final ApiDownloadFacade downloadFacade;

    @PostMapping("/ls")
    public List<ApiDownloadLsDTO.Response> ls(@RequestBody ApiDownloadLsDTO.Request request) {
        return downloadFacade.ls(request);
    }

    @PostMapping("/stop/{operationId}")
    public void stop(@PathVariable String operationId) {
        downloadFacade.stop(operationId);
    }

    @PostMapping("/stop-all")
    public void stopAll() {
        downloadFacade.stopAll();
    }

    @DeleteMapping("/rm/{operationId}")
    public void rm(@PathVariable String operationId) {
        downloadFacade.rm(operationId);
    }

    @DeleteMapping("/rm-all")
    public void rmAll() {
        downloadFacade.rmAll();
    }
}
