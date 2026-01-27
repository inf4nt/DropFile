package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.common.dto.ApiDownloadFileResponse;
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

    @GetMapping("/ls")
    public List<ApiDownloadFileResponse> ls() {
        return downloadFacade.ls();
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
        boolean removed = downloadFacade.rm(operationId);
        return removed ? ResponseEntity.ok().build() : ResponseEntity.status(404).body("No operation found");
    }

    @DeleteMapping("/rm-all")
    public ResponseEntity<Void> rmAll() {
        downloadFacade.rmAll();
        return ResponseEntity.ok().build();
    }
}
