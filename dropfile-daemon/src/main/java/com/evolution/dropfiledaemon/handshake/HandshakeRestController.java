package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfiledaemon.handshake.dto.HandshakeRequestDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeResponseDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeSessionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/handshake")
public class HandshakeRestController {

    private final HandshakeFacade handshakeFacade;

    @Autowired
    public HandshakeRestController(HandshakeFacade handshakeFacade) {
        this.handshakeFacade = handshakeFacade;
    }

    @PostMapping
    public synchronized HandshakeResponseDTO handshake(@RequestBody HandshakeRequestDTO requestDTO) {
        return handshakeFacade.handshake(requestDTO);
    }

    @PostMapping("/session")
    public synchronized HandshakeSessionDTO.Session sessionHandshake(@RequestBody HandshakeSessionDTO.Session sessionDTO) {
        return handshakeFacade.handshakeSession(sessionDTO);
    }
}
