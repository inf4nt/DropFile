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
    public AccessKeyInfoResponseDTO generateAccessKeys(@RequestBody AccessKeyGenerateRequestDTO requestDTO) {
        return apiFacade.generateAccessKeys(requestDTO);
    }

    @GetMapping("/connections/access/ls")
    public List<AccessKeyInfoResponseDTO> getAccessKeys() {
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

    @GetMapping("/connections/files/ls")
    public ResponseEntity<List<FileEntryResponseDTO>> connectionsLsFiles() {
        List<FileEntryResponseDTO> files = apiFacade.connectionsGetFiles();
        return ResponseEntity.ok(files);
    }

    @PostMapping("/connections/files/download")
    public ResponseEntity<ApiConnectionsDownloadFileResponseDTO> connectionsDownloadFile(@RequestBody ApiConnectionsDownloadFileRequestDTO requestDTO) {
        ApiConnectionsDownloadFileResponseDTO responseDTO = apiFacade.connectionsDownloadFile(requestDTO);
        if (responseDTO != null) {
            return ResponseEntity.ok(responseDTO);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/share/add")
    public ApiFileInfoResponseDTO addShareFile(@RequestBody ApiFileAddRequestDTO requestDTO) {
        return apiFacade.addFile(requestDTO);
    }

    @GetMapping("/share/ls")
    public List<ApiFileInfoResponseDTO> getShareFiles() {
        return apiFacade.getFiles();
    }

    @DeleteMapping("/share/rm/{id}")
    public ResponseEntity<ApiFileInfoResponseDTO> rmShareFile(@PathVariable String id) {
        ApiFileInfoResponseDTO responseDTO = apiFacade.deleteFile(id);
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
