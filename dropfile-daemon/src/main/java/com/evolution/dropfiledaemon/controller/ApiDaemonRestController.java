package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.common.dto.DaemonInfoResponseDTO;
import com.evolution.dropfiledaemon.facade.ApiDaemonFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/daemon")
public class ApiDaemonRestController {

    private final ApiDaemonFacade apiDaemonFacade;

    @PostMapping("/shutdown")
    public void shutdown() {
        apiDaemonFacade.shutdown();
    }

    @GetMapping("/info")
    public DaemonInfoResponseDTO info() {
        return apiDaemonFacade.info();
    }

    @PostMapping("/cache-reset")
    public void cacheReset() {
        apiDaemonFacade.cacheReset();
    }
}
