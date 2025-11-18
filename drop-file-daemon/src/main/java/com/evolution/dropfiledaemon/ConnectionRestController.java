package com.evolution.dropfiledaemon;

import com.evolution.dropfiledaemon.client.NodeHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    public Integer connect(@RequestBody String ip) {
        System.out.println("Connecting to " + ip);
        URI connection = URI.create(ip);
        HttpResponse<Void> httpResponse = nodeHttpClient.connect(connection);
        if (httpResponse.statusCode() == 200) {
            connectionSession.setConnection(connection);
        }
        return httpResponse.statusCode();
    }

    @GetMapping("/connect/status")
    public String status() {
        URI connection = connectionSession.getConnection();
        return connection.toString();
    }

    @PostMapping("/disconnect")
    public HttpStatus disconnect() {
        System.out.println("Disconnecting " + connectionSession.getConnection());
        connectionSession.setConnection(null);
        return HttpStatus.OK;
    }
}
