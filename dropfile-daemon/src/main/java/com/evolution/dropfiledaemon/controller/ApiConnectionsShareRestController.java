package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.common.dto.ApiConnectionsShareDownloadRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsShareDownloadResponseDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsShareLsResponseDTO;
import com.evolution.dropfiledaemon.facade.ApiConnectionsShareFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/connections/share")
public class ApiConnectionsShareRestController {

    private final ApiConnectionsShareFacade apiFacade;

    @Autowired
    public ApiConnectionsShareRestController(ApiConnectionsShareFacade apiFacade) {
        this.apiFacade = apiFacade;
    }

    @GetMapping("/ls")
    public ResponseEntity<List<ApiConnectionsShareLsResponseDTO>> connectionsShareLs() {
        List<ApiConnectionsShareLsResponseDTO> files = apiFacade.ls();
        return ResponseEntity.ok(files);
    }

    @PostMapping("/download")
    public ResponseEntity<ApiConnectionsShareDownloadResponseDTO> connectionsShareDownload(@RequestBody ApiConnectionsShareDownloadRequestDTO requestDTO) {
        ApiConnectionsShareDownloadResponseDTO responseDTO = apiFacade.download(requestDTO);
        if (responseDTO != null) {
            return ResponseEntity.ok(responseDTO);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/cat/{id}")
    public ResponseEntity<String> connectionsShareCat(@PathVariable String id) {
        String responseDTO = apiFacade.cat(id);
        if (responseDTO != null) {
            return ResponseEntity.ok(responseDTO);
        }
        return ResponseEntity.notFound().build();
    }
}
