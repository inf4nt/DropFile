package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.dto.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/handshake")
public class ApiHandshakeRestController {

    private final ApiHandshakeFacade apiHandshakeFacade;

    public ApiHandshakeRestController(ApiHandshakeFacade apiHandshakeFacade) {
        this.apiHandshakeFacade = apiHandshakeFacade;
    }

    @GetMapping("/request/incoming")
    public List<HandshakeApiIncomingResponseDTO> getIncomingRequests() {
        return apiHandshakeFacade.getIncomingRequests();
    }

    @GetMapping("/request/outgoing")
    public List<HandshakeApiOutgoingResponseDTO> getOutgoingRequests() {
        return apiHandshakeFacade.getOutgoingRequests();
    }

    @GetMapping("/trust/in")
    public List<HandshakeApiTrustInResponseDTO> getTrustIn() {
        return apiHandshakeFacade.getTrustIt();
    }

    @GetMapping("/trust/out")
    public List<HandshakeApiTrustOutResponseDTO> getTrustOut() {
        return apiHandshakeFacade.getTrustOut();
    }

    @PostMapping("/trust/{fingerprint}")
    public void trust(@PathVariable String fingerprint) {
        apiHandshakeFacade.trust(fingerprint);
    }

    @PostMapping("/request")
    public String handshakeRequest(
            @RequestBody HandshakeApiRequestBodyDTO requestBody) {
        return apiHandshakeFacade.initializeRequest(requestBody).name();
    }
}
