package com.evolution.dropfilecli.command.handshake;

import com.evolution.dropfile.common.dto.HandshakeApiRequestStatus;
import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "request",
        description = "Handshake request"
)
public class HandshakeRequestCommand implements CommandHttpHandler<byte[]> {

    private final DaemonClient daemonClient;

    private final ObjectMapper objectMapper;

    @CommandLine.Parameters(index = "0", description = "Address")
    private String address;

    @Autowired
    public HandshakeRequestCommand(DaemonClient daemonClient,
                                   ObjectMapper objectMapper) {
        this.daemonClient = daemonClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public HttpResponse<byte[]> execute() {
        return daemonClient.handshakeRequest(address);
    }

    @Override
    @SneakyThrows
    public void handleSuccessful(HttpResponse<byte[]> response) {
        HandshakeApiRequestStatus status = objectMapper.readValue(response.body(), HandshakeApiRequestStatus.class);
        System.out.println("Status: " + status);
    }
}
