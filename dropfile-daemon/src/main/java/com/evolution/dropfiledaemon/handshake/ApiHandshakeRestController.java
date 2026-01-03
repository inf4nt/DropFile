package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/handshake")
public class ApiHandshakeRestController {

    private final ApiHandshakeFacade apiHandshakeFacade;

    public ApiHandshakeRestController(ApiHandshakeFacade apiHandshakeFacade) {
        this.apiHandshakeFacade = apiHandshakeFacade;
    }

    @PostMapping("/identity")
    public HandshakeIdentityResponseDTO identity(@RequestBody HandshakeIdentityRequestDTO requestDTO) {
        return apiHandshakeFacade.identity(requestDTO.address());
    }

    @GetMapping("/trust/in")
    public List<HandshakeApiTrustInResponseDTO> getTrustIn() {
        return apiHandshakeFacade.getTrustIt();
    }

    @GetMapping("/trust/out")
    public List<HandshakeApiTrustOutResponseDTO> getTrustOut() {
        return apiHandshakeFacade.getTrustOut();
    }

    @GetMapping("/trust/out/{fingerprint}")
    public ResponseEntity<HandshakeApiTrustOutResponseDTO> getTrustOut(@PathVariable String fingerprint) {
        return apiHandshakeFacade.getTrustOut(fingerprint)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/trust/out/latest")
    public ResponseEntity<HandshakeApiTrustOutResponseDTO> getLatestTrustOut() {
        Optional<HandshakeApiTrustOutResponseDTO> latest = apiHandshakeFacade.getLatestTrustOut();
        return latest.map(it -> ResponseEntity.ok().body(it))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Void> doHandshake(@RequestBody ApiHandshakeRequestDTO requestDTO) {
        apiHandshakeFacade.doHandshake(requestDTO);
        return ResponseEntity.ok().build();
    }

    @Deprecated
    @PostMapping("/trust/{fingerprint}")
    public void trust(@PathVariable String fingerprint) {
        apiHandshakeFacade.trust(fingerprint);
    }

    @Deprecated
    @PostMapping("/request")
    public ResponseEntity<String> handshakeRequest(
            @RequestBody HandshakeApiRequestBodyDTO requestBody) {
        HandshakeApiRequestResponseStatus status = apiHandshakeFacade.handshake(requestBody);
        return ResponseEntity.ok(status.name());
    }

    @Deprecated
    @GetMapping("/request/incoming")
    public List<HandshakeApiIncomingResponseDTO> getIncomingRequests() {
        return apiHandshakeFacade.getIncomingRequests();
    }

    @Deprecated
    @GetMapping("/request/outgoing")
    public List<HandshakeApiOutgoingResponseDTO> getOutgoingRequests() {
        return apiHandshakeFacade.getOutgoingRequests();
    }

    @Deprecated
    @GetMapping("/request/outgoing/{fingerprint}")
    public ResponseEntity<HandshakeApiOutgoingResponseDTO> getOutgoingRequests(@PathVariable String fingerprint) {
        return apiHandshakeFacade.getOutgoingRequest(fingerprint)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
