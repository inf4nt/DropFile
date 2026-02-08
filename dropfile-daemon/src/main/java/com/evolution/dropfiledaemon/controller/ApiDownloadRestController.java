package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.common.dto.ApiDownloadLsDTO;
import com.evolution.dropfiledaemon.facade.ApiDownloadFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<String> stop(@PathVariable String operationId) {
        boolean stopped = downloadFacade.stop(operationId);
        if (stopped) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(404)
                .body("No operation to stop");
    }

    @PostMapping("/stop-all")
    public ResponseEntity<Void> stopAll() {
        downloadFacade.stopAll();
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/rm/{operationId}")
    public ResponseEntity<String> rm(@PathVariable String operationId) {
        downloadFacade.rm(operationId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/rm-all")
    public ResponseEntity<Void> rmAll() {
        downloadFacade.rmAll();
        return ResponseEntity.ok().build();
    }
}
