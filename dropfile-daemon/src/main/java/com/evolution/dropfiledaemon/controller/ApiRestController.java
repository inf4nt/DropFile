package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.common.dto.DaemonInfoResponseDTO;
import com.evolution.dropfiledaemon.facade.ApiFacade;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ApiRestController {

    private final ApiFacade apiFacade;

    public ApiRestController(ApiFacade apiFacade) {
        this.apiFacade = apiFacade;
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    @PostMapping("/shutdown")
    public void shutdown() {
        apiFacade.shutdown();
    }

    @GetMapping("/info")
    public DaemonInfoResponseDTO getInfo() {
        return apiFacade.getDaemonInfo();
    }

    @GetMapping("/node/ping/{fingerprint}")
    public String nodePing(@PathVariable String fingerprint) {
        return apiFacade.nodePing(fingerprint);
    }
}
