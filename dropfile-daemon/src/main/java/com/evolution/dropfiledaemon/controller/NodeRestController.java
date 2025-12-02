package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.configuration.dto.NodeConnectionsConnectionDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/node")
public class NodeRestController {

    @PostMapping("/connections/connect")
    public ResponseEntity<?> connect(@RequestBody NodeConnectionsConnectionDTO nodeConnectionsConnectionDTO) {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}
