package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.configuration.dto.HandshakeApproveDTO;
import com.evolution.dropfile.configuration.dto.HandshakeInfoDTO;
import com.evolution.dropfile.configuration.dto.HandshakeRequestDTO;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStoreManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/handshake")
public class HandshakeRestController {

    private final HandshakeFacade handshakeFacade;

    @Autowired
    public HandshakeRestController(HandshakeFacade handshakeFacade) {
        this.handshakeFacade = handshakeFacade;
    }

    @GetMapping("/status")
    public Map<String, List<HandshakeInfoDTO>> getStatus() {
        return handshakeFacade.getStatus();
    }

    @PostMapping("/request")
    public ResponseEntity<Void> request(@RequestBody HandshakeRequestDTO requestDTO) {
        handshakeFacade.request(requestDTO);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{fingerprint}")
    public ResponseEntity<HandshakeApproveDTO> status(@PathVariable String fingerprint) {
        Optional<HandshakeApproveDTO> handshakeStatus = handshakeFacade.getHandshakeApprove(fingerprint);
        return handshakeStatus
                .map(it -> new ResponseEntity<>(it, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/approve/{fingerprint}")
    public ResponseEntity<HandshakeApproveDTO> approve(@PathVariable String fingerprint) {
        HandshakeApproveDTO responseDTO = handshakeFacade.approve(fingerprint);
        return ResponseEntity.ok(responseDTO);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = {
            HandshakeStoreManager.NoRequestException.class,
            HandshakeStoreManager.AlreadyTrustException.class
    })
    public void handleExceptions() {
    }
}
