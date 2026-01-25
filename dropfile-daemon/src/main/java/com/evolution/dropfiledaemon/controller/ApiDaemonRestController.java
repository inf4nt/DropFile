package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.common.dto.DaemonInfoResponseDTO;
import com.evolution.dropfiledaemon.facade.ApiDaemonFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/daemon")
public class ApiDaemonRestController {

    private final ApiDaemonFacade apiDaemonFacade;

    @Autowired
    public ApiDaemonRestController(ApiDaemonFacade apiDaemonFacade) {
        this.apiDaemonFacade = apiDaemonFacade;
    }

    @PostMapping("/shutdown")
    public void shutdown() {
        apiDaemonFacade.shutdown();
    }

    @GetMapping("/info")
    public DaemonInfoResponseDTO info() {
        return apiDaemonFacade.info();
    }

}
