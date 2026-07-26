package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.common.dto.TunnelTrafficResponseDTO;
import com.evolution.dropfiledaemon.facade.ApiConnectionsFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
