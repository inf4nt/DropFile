package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.dto.ApiHandshakeReconnectRequestDTO;
import com.evolution.dropfile.common.dto.ApiHandshakeRequestDTO;
import com.evolution.dropfile.common.dto.ApiHandshakeStatusResponseDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

//    @GetMapping("/trust/in")
//    public List<HandshakeApiTrustInResponseDTO> getTrustIn() {
//        return apiHandshakeFacade.getTrustIt();
//    }
//
//    @GetMapping("/trust/out")
//    public List<HandshakeApiTrustOutResponseDTO> getTrustOut() {
//        return apiHandshakeFacade.getTrustOut();
//    }
//
//    @GetMapping("/trust/out/latest")
//    public HandshakeApiTrustOutResponseDTO getLatestTrustOut() {
//        return apiHandshakeFacade.getLatestTrustOut();
//    }
}
