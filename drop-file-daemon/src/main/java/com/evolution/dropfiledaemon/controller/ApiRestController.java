package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.configuration.dto.ConnectionsConnectionDTO;
import com.evolution.dropfile.configuration.dto.ConnectionsConnectionResultDTO;
import com.evolution.dropfile.configuration.dto.ConnectionsOnline;
import com.evolution.dropfiledaemon.facade.ConnectionsFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ApiRestController {

    private final ConnectionsFacade connectionsFacade;

    @Autowired
    public ApiRestController(ConnectionsFacade connectionsFacade) {
        this.connectionsFacade = connectionsFacade;
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    @GetMapping("/connections/online")
    public ConnectionsOnline online() {
        return connectionsFacade.getOnlineConnections();
    }

    @PostMapping("/connections/connect")
    public ConnectionsConnectionResultDTO connect(@RequestBody ConnectionsConnectionDTO connectionsConnectionDTO) {
        String connectionAddress = connectionsConnectionDTO.getConnectionAddress();
        String resultConnectionId = connectionsFacade
                .connect(connectionAddress);
        return new ConnectionsConnectionResultDTO(resultConnectionId, connectionAddress);
    }
}
