package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.common.dto.ApiConnectionsShareDownloadRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsShareDownloadResponseDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsShareLsRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsShareLsResponseDTO;
import com.evolution.dropfiledaemon.facade.ApiConnectionsShareFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/connections/share")
public class ApiConnectionsShareRestController {

    private final ApiConnectionsShareFacade apiFacade;

    @PostMapping("/ls")
    public ResponseEntity<List<ApiConnectionsShareLsResponseDTO>> ls(@RequestBody ApiConnectionsShareLsRequestDTO requestDTO) {
        List<ApiConnectionsShareLsResponseDTO> files = apiFacade.ls(requestDTO);
        return ResponseEntity.ok(files);
    }

    @PostMapping("/download")
    public ResponseEntity<ApiConnectionsShareDownloadResponseDTO> download(@RequestBody ApiConnectionsShareDownloadRequestDTO requestDTO) {
        return ResponseEntity.ok(apiFacade.download(requestDTO));
    }

    @GetMapping("/cat/{id}")
    public ResponseEntity<String> cat(@PathVariable String id) {
        String responseDTO = apiFacade.cat(id);
        return ResponseEntity.ok(responseDTO);
    }
}
