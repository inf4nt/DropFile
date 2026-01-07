package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.common.dto.*;
import com.evolution.dropfiledaemon.facade.ApiFacade;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiRestController {

    private final ApiFacade apiFacade;

    public ApiRestController(ApiFacade apiFacade) {
        this.apiFacade = apiFacade;
    }

    @GetMapping("/daemon/ping")
    public String ping() {
        return "pong";
    }

    @PostMapping("/daemon/shutdown")
    public void shutdown() {
        apiFacade.shutdown();
    }

    @GetMapping("/daemon/info")
    public DaemonInfoResponseDTO getInfo() {
        return apiFacade.getDaemonInfo();
    }

    @PostMapping("/connections/access/generate")
    public ApiConnectionsAccessInfoResponseDTO generateAccessKeys(@RequestBody ApiConnectionsAccessGenerateRequestDTO requestDTO) {
        return apiFacade.generateAccessKeys(requestDTO);
    }

    @GetMapping("/connections/access/ls")
    public List<ApiConnectionsAccessInfoResponseDTO> getAccessKeys() {
        return apiFacade.getAccessKeys();
    }

    @DeleteMapping("/connections/access/rm/{id}")
    public ResponseEntity<Void> rmAccessKey(@PathVariable String id) {
        boolean result = apiFacade.rmAccessKey(id);
        if (result) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/connections/access/rm-all")
    public ResponseEntity<Void> rmAllAccessKeys() {
        apiFacade.rmAllAccessKeys();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/connections/share/ls")
    public ResponseEntity<List<ApiConnectionsShareLsResponseDTO>> connectionsShareLs() {
        List<ApiConnectionsShareLsResponseDTO> files = apiFacade.connectionsShareLs();
        return ResponseEntity.ok(files);
    }

    @PostMapping("/connections/share/download")
    public ResponseEntity<ApiConnectionsShareDownloadResponseDTO> connectionsShareDownload(@RequestBody ApiConnectionsShareDownloadRequestDTO requestDTO) {
        ApiConnectionsShareDownloadResponseDTO responseDTO = apiFacade.connectionsShareDownload(requestDTO);
        if (responseDTO != null) {
            return ResponseEntity.ok(responseDTO);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/connections/share/cat/{id}")
    public ResponseEntity<String> connectionsShareCat(@PathVariable String id) {
        String responseDTO = apiFacade.connectionsShareCat(id);
        if (responseDTO != null) {
            return ResponseEntity.ok(responseDTO);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/share/add")
    public ApiShareInfoResponseDTO addShareFile(@RequestBody ApiShareAddRequestDTO requestDTO) {
        return apiFacade.addFile(requestDTO);
    }

    @GetMapping("/share/ls")
    public List<ApiShareInfoResponseDTO> getShareFiles() {
        return apiFacade.getFiles();
    }

    @DeleteMapping("/share/rm/{id}")
    public ResponseEntity<ApiShareInfoResponseDTO> rmShareFile(@PathVariable String id) {
        ApiShareInfoResponseDTO responseDTO = apiFacade.deleteFile(id);
        if (responseDTO != null) {
            return ResponseEntity.ok(responseDTO);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/share/rm-all")
    public ResponseEntity<Void> rmAllShareFiles() {
        apiFacade.deleteAllFiles();
        return ResponseEntity.ok().build();
    }
}
