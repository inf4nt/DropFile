package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.configuration.dto.HandshakeDTO;
import com.evolution.dropfile.configuration.dto.HandshakeRequestApprovedDTO;
import com.evolution.dropfile.configuration.dto.HandshakeRequestDTO;
import com.evolution.dropfiledaemon.InMemoryHandshakeStore;
import com.evolution.dropfiledaemon.facade.HandshakeFacade;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/handshake")
public class HandshakeRestController {

    @Autowired
    private HandshakeFacade handshakeFacade;

    @Autowired
    private InMemoryHandshakeStore handshakeStore;

    @GetMapping("/{fingerprint}")
    public ResponseEntity<HandshakeDTO> getHandshake(@PathVariable String fingerprint) {
        Optional<HandshakeDTO> handshakeDTO = handshakeFacade.getHandshakeDTO(fingerprint);
        return handshakeDTO
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @SneakyThrows
    @PostMapping("/request")
    public ResponseEntity<Void> handshakeRequest(@RequestBody HandshakeRequestDTO requestBody,
                                                 HttpServletRequest httpServletRequest) {
        handshakeFacade.processRequest(requestBody, httpServletRequest.getRemoteAddr());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public Object getRequests() {
        return Map.of(
                "requests", handshakeStore.getRequests(),
                "trusted", handshakeStore.getTrusted()
        );
    }

    @SneakyThrows
    @PostMapping("/request/approve/{fingerprint}")
    public ResponseEntity<Void> requestApprove(@PathVariable String fingerprint) {
        handshakeFacade.approve(fingerprint);
        return ResponseEntity.ok().build();
    }

    @SneakyThrows
    @PostMapping("/request/approved")
    public ResponseEntity<Void> requestApproved(@RequestBody HandshakeRequestApprovedDTO requestBody) {
        handshakeFacade.finalizeApprove(requestBody);
        return ResponseEntity.ok().build();
    }
}
