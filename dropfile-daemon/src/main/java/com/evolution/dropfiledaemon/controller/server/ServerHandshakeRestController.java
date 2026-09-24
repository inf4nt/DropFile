package com.evolution.dropfiledaemon.controller.server;

import com.evolution.dropfiledaemon.handshake.HandshakeFacade;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeRequestDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeResponseDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeSessionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.WebAsyncTask;

@RequiredArgsConstructor
@RestController
@RequestMapping
public class ServerHandshakeRestController {

    public static final String HANDSHAKE_ENDPOINT = "/s/h";

    public static final String HANDSHAKE_SESSION_ENDPOINT = "/s/h/s";

    private final HandshakeFacade handshakeFacade;

    @PostMapping(ServerHandshakeRestController.HANDSHAKE_ENDPOINT)
    public WebAsyncTask<HandshakeResponseDTO> handshake(@RequestBody HandshakeRequestDTO requestDTO) {
        return new WebAsyncTask<>(() -> handshakeFacade.handshake(requestDTO));
    }

    @PostMapping(ServerHandshakeRestController.HANDSHAKE_SESSION_ENDPOINT)
    public WebAsyncTask<HandshakeSessionDTO.Session> sessionHandshake(@RequestBody HandshakeSessionDTO.Session sessionDTO) {
        return new WebAsyncTask<>(() -> handshakeFacade.handshakeSession(sessionDTO));
    }
}
