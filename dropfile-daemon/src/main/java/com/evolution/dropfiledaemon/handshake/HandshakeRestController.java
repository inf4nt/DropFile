package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.dto.*;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;

@RestController
@RequestMapping("/handshake")
public class HandshakeRestController {

    private final HandshakeFacade handshakeFacade;

    @Autowired
    public HandshakeRestController(HandshakeFacade handshakeFacade) {
        this.handshakeFacade = handshakeFacade;
    }

    @Autowired
    private HandshakeStore handshakeStore;

    @GetMapping
    public Object getAll() {
        return new LinkedHashMap<String, Object>() {{
            put("incoming", handshakeStore.incomingRequestStore().getAll());
            put("outgoing", handshakeStore.outgoingRequestStore().getAll());
            put("allowedIn", handshakeStore.allowedInStore().getAll());
            put("allowedOut", handshakeStore.allowedOutStore().getAll());
        }};
    }

//    @GetMapping("/request")
//    public List<HandshakeStatusInfoDTO> getRequests() {
//        return handshakeFacade.getRequests();
//    }

    @PostMapping("/request")
    public ResponseEntity<HandshakeRequestResponseDTO> request(@RequestBody HandshakeRequestDTO requestDTO) {
        ;
        return ResponseEntity.ok(handshakeFacade.request(requestDTO));
    }

//    @GetMapping("/trust")
//    public List<HandshakeStatusInfoDTO> getTrust() {
//        return handshakeFacade.getTrusts();
//    }

    @GetMapping("/trust/{fingerprint}")
    public ResponseEntity<HandshakeTrustDTO> getTrustStatus(@PathVariable String fingerprint) {
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
}
