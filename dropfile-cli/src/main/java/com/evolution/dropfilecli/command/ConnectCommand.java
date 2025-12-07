package com.evolution.dropfilecli.command;

import com.evolution.dropfile.common.dto.HandshakeApiRequestResponseStatus;
import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "connect",
        description = "Connect to a given node"
)
public class ConnectCommand implements CommandHttpHandler<byte[]> {

    @CommandLine.Parameters(index = "0", description = "Address")
    private String address;

    @CommandLine.Parameters(index = "1", description = "Timeout in seconds", defaultValue = "60")
    private Integer timeout;

    private final DaemonClient daemonClient;

    private final ObjectMapper objectMapper;

    @Autowired
    public ConnectCommand(DaemonClient daemonClient, ObjectMapper objectMapper) {
        this.daemonClient = daemonClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public HttpResponse<byte[]> execute() {
        return daemonClient.handshakeRequest(address, timeout);
    }

    @Override
    public void handleSuccessful(HttpResponse<byte[]> response) throws Exception {
        HandshakeApiRequestResponseStatus status = objectMapper
                .readValue(response.body(), HandshakeApiRequestResponseStatus.class);
        System.out.println("Status: " + status);
    }
}
