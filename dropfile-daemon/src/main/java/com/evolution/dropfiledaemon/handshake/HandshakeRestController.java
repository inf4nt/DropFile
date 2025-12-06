package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.configuration.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/handshake")
public class HandshakeRestController {

    private final HandshakeFacade handshakeFacade;

    @Autowired
    public HandshakeRestController(HandshakeFacade handshakeFacade) {
        this.handshakeFacade = handshakeFacade;
    }

    @GetMapping("/request")
    public List<HandshakeStatusInfoDTO> getRequests() {
        return handshakeFacade.getRequests();
    }

    @PostMapping("/request")
    public ResponseEntity<Void> request(@RequestBody HandshakeRequestDTO requestDTO) {
        handshakeFacade.request(requestDTO);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/trust")
    public List<HandshakeStatusInfoDTO> getTrust() {
        return handshakeFacade.getTrusts();
    }

    @GetMapping("/trust/{fingerprint}")
    public ResponseEntity<HandshakeTrustDTO> trustStatus(@PathVariable String fingerprint) {
        return handshakeFacade
                .getHandshakeApprove(fingerprint)
                .map(it -> ResponseEntity.ok().body(it))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/trust/{fingerprint}")
    public ResponseEntity<Void> approve(@PathVariable String fingerprint) {
        handshakeFacade.trust(fingerprint);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/challenge")
    public ResponseEntity<HandshakeChallengeResponseDTO> challenge(
            @RequestBody HandshakeChallengeRequestDTO requestDTO) {
        return ResponseEntity.ok(handshakeFacade.challenge(requestDTO));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = {
            NoSuchElementException.class
    })
    public void noSuchElementExceptionToBadRequest() {
    }
}
