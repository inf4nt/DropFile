package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.common.dto.ConnectionsConnectionDTO;
import com.evolution.dropfile.common.dto.ConnectionsConnectionResultDTO;
import com.evolution.dropfile.common.dto.ConnectionsOnline;
import com.evolution.dropfiledaemon.facade.ConnectionsFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/daemon")
public class DaemonRestController {

    private final ConnectionsFacade connectionsFacade;

    @Autowired
    public DaemonRestController(ConnectionsFacade connectionsFacade) {
        this.connectionsFacade = connectionsFacade;
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
