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

    @PostMapping
    public ApiHandshakeStatusResponseDTO handshake(
            @RequestBody ApiHandshakeRequestDTO requestDTO) {
        return apiHandshakeFacade.handshake(requestDTO);
    }

    @PostMapping("/reconnect")
    public ApiHandshakeStatusResponseDTO handshakeReconnect(@RequestBody ApiHandshakeReconnectRequestDTO requestDTO) {
        return apiHandshakeFacade.handshakeReconnect(requestDTO);
    }

    @PostMapping("/status")
    public ApiHandshakeStatusResponseDTO handshakeStatus() {
        return apiHandshakeFacade.handshakeStatus();
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

    @GetMapping("/trust/out/latest")
    public ResponseEntity<HandshakeApiTrustOutResponseDTO> getLatestTrustOut() {
        Optional<HandshakeApiTrustOutResponseDTO> latest = apiHandshakeFacade.getLatestTrustOut();
        return latest.map(it -> ResponseEntity.ok().body(it))
                .orElse(ResponseEntity.notFound().build());
    }
}
