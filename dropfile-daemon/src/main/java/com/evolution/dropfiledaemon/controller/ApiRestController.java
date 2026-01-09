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
    public void daemonShutdown() {
        apiFacade.daemonShutdown();
    }

    @GetMapping("/daemon/info")
    public DaemonInfoResponseDTO daemonInfo() {
        return apiFacade.daemonInfo();
    }

    @PostMapping("/connections/access/generate")
    public ApiConnectionsAccessInfoResponseDTO connectionsAccessGenerate(@RequestBody ApiConnectionsAccessGenerateRequestDTO requestDTO) {
        return apiFacade.connectionsAccessGenerate(requestDTO);
    }

    @GetMapping("/connections/access/ls")
    public List<ApiConnectionsAccessInfoResponseDTO> connectionsAccessLs() {
        return apiFacade.connectionsAccessLs();
    }

    @DeleteMapping("/connections/access/rm/{id}")
    public ResponseEntity<Void> connectionsAccessRm(@PathVariable String id) {
        boolean result = apiFacade.connectionsAccessRm(id);
        if (result) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/connections/access/rm-all")
    public ResponseEntity<Void> connectionsAccessRmAll() {
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
        return apiFacade.shareAdd(requestDTO);
    }

    @GetMapping("/share/ls")
    public List<ApiShareInfoResponseDTO> getShareFiles() {
        return apiFacade.shareLs();
    }

    @DeleteMapping("/share/rm/{id}")
    public ResponseEntity<ApiShareInfoResponseDTO> rmShareFile(@PathVariable String id) {
        ApiShareInfoResponseDTO responseDTO = apiFacade.shareRm(id);
        if (responseDTO != null) {
            return ResponseEntity.ok(responseDTO);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/share/rm-all")
    public ResponseEntity<Void> rmAllShareFiles() {
        apiFacade.shareRmAll();
        return ResponseEntity.ok().build();
    }
}
