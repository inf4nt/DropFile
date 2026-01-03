package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.dto.HandshakeRequestDTO;
import com.evolution.dropfile.common.dto.HandshakeResponseDTO;
import com.evolution.dropfile.common.dto.HandshakeIdentityResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/handshake")
public class HandshakeRestController {

    private final HandshakeFacade handshakeFacade;

    @Autowired
    public HandshakeRestController(HandshakeFacade handshakeFacade) {
        this.handshakeFacade = handshakeFacade;
    }

    @GetMapping
    public HandshakeIdentityResponseDTO getIdentity() {
        return handshakeFacade.getHandshakeIdentity();
    }

    @PostMapping
    public HandshakeResponseDTO handshake(@RequestBody HandshakeRequestDTO requestDTO) {
        return handshakeFacade.handshake(requestDTO);
    }
}
