package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.common.dto.TunnelTrafficResponseDTO;
import com.evolution.dropfiledaemon.facade.ApiConnectionsFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/connections")
public class ApiConnectionsRestController {

    private final ApiConnectionsFacade apiFacade;

    @PostMapping("/revoke/fingerprint/{fingerprint}")
    public void revoke(@PathVariable String fingerprint) {
        apiFacade.revoke(fingerprint);
    }

    @PostMapping("/revoke/all")
    public void revokeAll() {
        apiFacade.revokeAll();
    }

    @PostMapping("/disconnect/fingerprint/{fingerprint}")
    public void disconnect(@PathVariable String fingerprint) {
        apiFacade.disconnect(fingerprint);
    }

    @PostMapping("/disconnect/current")
    public void disconnectCurrent() {
        apiFacade.disconnectCurrent();
    }

    @PostMapping("/disconnect/all")
    public void disconnectAll() {
        apiFacade.disconnectAll();
    }

    @GetMapping("/traffic")
    public List<TunnelTrafficResponseDTO> getTraffic() {
        return apiFacade.getTraffic();
    }
}
