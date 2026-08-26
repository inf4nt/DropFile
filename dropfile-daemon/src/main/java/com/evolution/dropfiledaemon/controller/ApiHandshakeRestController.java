package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.common.dto.*;
import com.evolution.dropfiledaemon.facade.ApiHandshakeFacade;
import com.evolution.dropfiledaemon.handshake.HandshakeFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/handshake")
public class ApiHandshakeRestController {

    private final ApiHandshakeFacade apiHandshakeFacade;

    private final HandshakeFacade handshakeFacade;

    @PostMapping
    public void handshake(@RequestBody ApiHandshakeRequestDTO requestDTO) {
        apiHandshakeFacade.handshake(requestDTO);
    }

    @PostMapping("/reconnect")
    public void handshakeReconnect(@RequestBody ApiHandshakeReconnectRequestDTO requestDTO) {
        apiHandshakeFacade.handshakeReconnect(requestDTO);
    }

    @PostMapping("/current/reconnect")
    public void handshakeCurrentReconnect() {
        apiHandshakeFacade.handshakeCurrentReconnect();
    }

    @GetMapping("/trust/in")
    public List<HandshakeApiTrustInResponseDTO> getTrustIn() {
        return handshakeFacade.getTrustIt();
    }

    @GetMapping("/trust/out")
    public List<HandshakeApiTrustOutResponseDTO> getTrustOut() {
        return apiHandshakeFacade.getTrustOut();
    }

    @GetMapping("/trust/out/latest")
    public HandshakeApiTrustOutResponseDTO getLatestTrustOut() {
        return apiHandshakeFacade.getLatestTrustOut();
    }

    @PostMapping("/revoke/fingerprint/{fingerprint}")
    public void revoke(@PathVariable String fingerprint) {
        handshakeFacade.revoke(fingerprint);
    }

    @PostMapping("/revoke/all")
    public void revokeAll() {
        handshakeFacade.revokeAll();
    }

    @PostMapping("/disconnect/fingerprint/{fingerprint}")
    public void disconnect(@PathVariable String fingerprint) {
        apiHandshakeFacade.disconnect(fingerprint);
    }

    @PostMapping("/disconnect/current")
    public void disconnectCurrent() {
        apiHandshakeFacade.disconnectCurrent();
    }

    @PostMapping("/disconnect/all")
    public void disconnectAll() {
        apiHandshakeFacade.disconnectAll();
    }
}
