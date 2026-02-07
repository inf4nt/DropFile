package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfile.common.PrintReflection;
import com.evolution.dropfile.common.dto.HandshakeApiTrustOutResponseDTO;
import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "current",
        description = "Retrieve current connection"
)
public class CurrentConnectionCommand implements CommandHttpHandler<byte[]> {

    private final DaemonClient daemonClient;

    private final ObjectMapper objectMapper;

    @Autowired
    public CurrentConnectionCommand(DaemonClient daemonClient,
                                    ObjectMapper objectMapper) {
        this.daemonClient = daemonClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.getTrustLatest();
    }

    @Override
    public void handleSuccessful(HttpResponse<byte[]> response) throws Exception {
        HandshakeApiTrustOutResponseDTO responseDTO = objectMapper.readValue(response.body(), HandshakeApiTrustOutResponseDTO.class);
        PrintReflection.print(responseDTO);
    }
}
