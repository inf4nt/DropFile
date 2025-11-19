package com.evolution.dropfiledaemon;

import com.evolution.dropfiledaemon.node.NodeHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpResponse;

@RestController
@RequestMapping("/daemon")
public class ConnectionRestController {

    @Autowired
    private final ConnectionSession connectionSession;

    private final NodeHttpClient nodeHttpClient;

    public ConnectionRestController(ConnectionSession connectionSession, NodeHttpClient nodeHttpClient) {
        this.connectionSession = connectionSession;
        this.nodeHttpClient = nodeHttpClient;
    }

    @PostMapping("/connect")
    public ResponseEntity<Void> connect(@RequestBody String ip) {
        System.out.println("Connecting to " + ip);
        URI connection = URI.create(ip);
        HttpResponse<Void> httpResponse = nodeHttpClient.connect(connection);
        if (httpResponse.statusCode() == 200) {
            connectionSession.setConnection(connection);
        }
        return ResponseEntity
                .status(httpResponse.statusCode())
                .build();
    }

    @GetMapping("/connect/status")
    public URI status() {
        return connectionSession.getConnection();
    }

    @PostMapping("/disconnect")
    public ResponseEntity<Void> disconnect() {
        URI connection = connectionSession.getConnection();
        System.out.println("Disconnecting " + connection);
        HttpResponse<Void> disconnect = nodeHttpClient.disconnect(connection);
        if (disconnect.statusCode() == 200) {
            connectionSession.setConnection(null);
            return ResponseEntity.ok().build();
        }
        return  ResponseEntity
                .status(disconnect.statusCode())
                .build();
    }
}
