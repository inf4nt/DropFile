package com.evolution.dropfiledaemon.tunnel;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tunnel")
public class TunnelRestController {

    private final TunnelDispatcher tunnelDispatcher;

    public TunnelRestController(TunnelDispatcher tunnelDispatcher) {
        this.tunnelDispatcher = tunnelDispatcher;
    }

    @PostMapping
    public ResponseEntity<TunnelResponseDTO> tunnel(@RequestBody TunnelRequestDTO requestDTO) {
        try {
            return ResponseEntity.ok(tunnelDispatcher.dispatch(requestDTO));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }
}
