package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.common.dto.AccessKeyGenerateRequestDTO;
import com.evolution.dropfile.common.dto.AccessKeyInfoResponseDTO;
import com.evolution.dropfile.common.dto.DaemonInfoResponseDTO;
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

    @PostMapping("/connections/access")
    public AccessKeyInfoResponseDTO generateAccessKeys(@RequestBody AccessKeyGenerateRequestDTO requestDTO) {
        return apiFacade.generateAccessKeys(requestDTO);
    }

    @GetMapping("/connections/access")
    public List<AccessKeyInfoResponseDTO> getAccessKeys() {
        return apiFacade.getAccessKeys();
    }

    @DeleteMapping("/connections/access/{id}")
    public ResponseEntity<Void> revokeAccessKey(@PathVariable String id) {
        AccessKeyInfoResponseDTO key = apiFacade.revokeAccessKey(id);
        if (key != null) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/connections/access")
    public ResponseEntity<Void> revokeAllAccessKeys() {
        apiFacade.revokeAllAccessKeys();
        return ResponseEntity.ok().build();
    }
}
