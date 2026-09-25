package com.evolution.dropfiledaemon.controller.api;

import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfile.common.dto.ApiHandshakeReconnectAddressRequestDTO;
import com.evolution.dropfile.common.dto.ApiHandshakeRequestDTO;
import com.evolution.dropfile.common.dto.HandshakeApiTrustInResponseDTO;
import com.evolution.dropfile.common.dto.HandshakeApiTrustOutResponseDTO;
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

    @PostMapping("/reconnect/address")
    public void handshakeReconnectAddress(@RequestBody ApiHandshakeReconnectAddressRequestDTO requestDTO) {
        apiHandshakeFacade.handshakeReconnectAddress(requestDTO);
    }

    @PostMapping("/reconnect/current")
    public void handshakeReconnectCurrent() {
        apiHandshakeFacade.handshakeReconnectCurrent();
    }

    @PostMapping("/reconnect/fingerprint")
    public void handshakeReconnectFingerprint(@RequestBody CriteriaEnvelope criteriaEnvelope) {
        apiHandshakeFacade.handshakeReconnectFingerprint(criteriaEnvelope);
    }

    @PostMapping("/reconnect/alias")
    public void handshakeReconnectAlias(@RequestBody CriteriaEnvelope criteriaEnvelopeAlias) throws Exception {
        apiHandshakeFacade.handshakeReconnectAlias(criteriaEnvelopeAlias);
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

    @PostMapping("/revoke/fingerprint")
    public void revoke(@RequestBody CriteriaEnvelope fingerprintCriteria) {
        handshakeFacade.revoke(fingerprintCriteria);
    }

    @PostMapping("/revoke/all")
    public void revokeAll() {
        handshakeFacade.revokeAll();
    }

    @PostMapping("/disconnect/fingerprint")
    public void disconnect(@RequestBody CriteriaEnvelope fingerprintCriteriaEnvelope) {
        apiHandshakeFacade.disconnect(fingerprintCriteriaEnvelope);
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
