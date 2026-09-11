package com.evolution.dropfiledaemon.controller.api;

import com.evolution.dropfile.common.CriteriaEnvelope;
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

    @GetMapping("/traffic")
    public List<TunnelTrafficResponseDTO> getTraffic() {
        return apiFacade.getTraffic();
    }

    @PostMapping("/tunnel/ping")
    public void tunnelPing(@RequestBody(required = false) CriteriaEnvelope fingerprintCriteria) {
        apiFacade.tunnelPing(fingerprintCriteria);
    }
}
