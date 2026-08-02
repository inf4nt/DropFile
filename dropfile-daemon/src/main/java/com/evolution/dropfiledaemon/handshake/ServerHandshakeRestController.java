package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfiledaemon.handshake.dto.HandshakeRequestDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeResponseDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeSessionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping
public class ServerHandshakeRestController {

    public static final String HANDSHAKE_ENDPOINT = "/s/h";

    public static final String HANDSHAKE_SESSION_ENDPOINT = "/s/h/s";

    private final HandshakeFacade handshakeFacade;

    @PostMapping(ServerHandshakeRestController.HANDSHAKE_ENDPOINT)
    public HandshakeResponseDTO handshake(@RequestBody HandshakeRequestDTO requestDTO) {
        return handshakeFacade.handshake(requestDTO);
    }

    @PostMapping(ServerHandshakeRestController.HANDSHAKE_SESSION_ENDPOINT)
    public HandshakeSessionDTO.Session sessionHandshake(@RequestBody HandshakeSessionDTO.Session sessionDTO) {
        return handshakeFacade.handshakeSession(sessionDTO);
    }
}
