package com.evolution.dropfiledaemon.old;

import com.evolution.dropfiledaemon.old.node.NodeHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpResponse;

@RestController
@RequestMapping("/daemon")
public class ConnectionRestController {

    private final ConnectionSession connectionSession;

    private final NodeHttpClient nodeHttpClient;

    @Autowired
    public ConnectionRestController(ConnectionSession connectionSession, NodeHttpClient nodeHttpClient) {
        this.connectionSession = connectionSession;
        this.nodeHttpClient = nodeHttpClient;
    }

    @GetMapping("/connect/online")
    public ResponseEntity<String> online() {
        URI connection = connectionSession.getConnection();
        if (connection == null) {
            return ResponseEntity.ok().build();
        }
        try {
            HttpResponse<String> httpResponsePing = nodeHttpClient.ping(connection);
            if (httpResponsePing.statusCode() == 200) {
                return ResponseEntity.ok()
                        .body("Online " + connection);
            }
        } catch (Exception e) {
            return ResponseEntity.ok()
                    .body("Offline " + connection);
        }

        return ResponseEntity.ok().build();
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
        if (connection == null) {
            return ResponseEntity.ok().build();
        }
        System.out.println("Disconnecting " + connection);
        // TODO node can be off
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
