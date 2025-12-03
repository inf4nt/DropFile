package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.configuration.dto.ConnectionsConnectionDTO;
import com.evolution.dropfile.configuration.dto.ConnectionsConnectionResultDTO;
import com.evolution.dropfile.configuration.dto.ConnectionsOnline;
import com.evolution.dropfile.configuration.dto.HandshakeApiRequestDTO;
import com.evolution.dropfiledaemon.facade.ConnectionsFacade;
import com.evolution.dropfiledaemon.facade.HandshakeFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api")
public class ApiRestController {

    private final ConnectionsFacade connectionsFacade;

    private final HandshakeFacade handshakeFacade;

    @Autowired
    public ApiRestController(ConnectionsFacade connectionsFacade,
                             HandshakeFacade handshakeFacade) {
        this.connectionsFacade = connectionsFacade;
        this.handshakeFacade = handshakeFacade;
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    @PostMapping("/shutdown")
    public void shutdown() {
        Executors.newSingleThreadExecutor()
                .submit(() -> {
                    System.exit(0);
                });
    }

    @GetMapping("/connections/online")
    public ConnectionsOnline online() {
        return connectionsFacade.getOnlineConnections();
    }

    @PostMapping("/connections/connect")
    public ConnectionsConnectionResultDTO connect(
            @RequestBody ConnectionsConnectionDTO connectionsConnectionDTO) {
        String connectionAddress = connectionsConnectionDTO.getConnectionAddress();
        String resultConnectionId = connectionsFacade
                .connect(connectionAddress);
        return new ConnectionsConnectionResultDTO(resultConnectionId, connectionAddress);
    }

    @PostMapping("/handshake/request")
    public ResponseEntity<Void> handshakeRequest(@RequestBody HandshakeApiRequestDTO requestBody) {
        handshakeFacade.initializeRequest(requestBody);
        return ResponseEntity.ok().build();
    }
}
