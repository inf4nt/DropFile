package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/handshake")
public class HandshakeRestController {

    private final HandshakeFacade handshakeFacade;

    @Autowired
    public HandshakeRestController(HandshakeFacade handshakeFacade) {
        this.handshakeFacade = handshakeFacade;
    }

    @PostMapping("/request")
    public ResponseEntity<HandshakeRequestResponseDTO> request(@RequestBody HandshakeRequestBodyDTO requestDTO) {
        return ResponseEntity.ok(handshakeFacade.request(requestDTO));
    }

    @GetMapping("/trust/{fingerprint}")
    public ResponseEntity<HandshakeTrustResponseDTO> getTrustStatus(@PathVariable String fingerprint) {
        return handshakeFacade
                .getHandshakeApprove(fingerprint)
                .map(it -> ResponseEntity.ok().body(it))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/challenge")
    public ResponseEntity<HandshakeChallengeResponseDTO> challenge(
            @RequestBody HandshakeChallengeRequestBodyDTO requestDTO) {
        return ResponseEntity.ok(handshakeFacade.challenge(requestDTO));
    }
}
