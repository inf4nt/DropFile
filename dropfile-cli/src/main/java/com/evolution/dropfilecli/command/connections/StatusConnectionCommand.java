package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfile.common.PrintReflection;
import com.evolution.dropfile.common.dto.ApiHandshakeStatusResponseDTO;
import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "status",
        description = "Retrieve status of current connection"
)
public class StatusConnectionCommand implements CommandHttpHandler<byte[]> {

    private final DaemonClient daemonClient;

    private final ObjectMapper objectMapper;

    @Autowired
    public StatusConnectionCommand(DaemonClient daemonClient,
                                   ObjectMapper objectMapper) {
        this.daemonClient = daemonClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.handshakeStatus();
    }

    @Override
    public void handleSuccessful(HttpResponse<byte[]> response) throws Exception {
        ApiHandshakeStatusResponseDTO responseDTO = objectMapper.readValue(
                response.body(),
                ApiHandshakeStatusResponseDTO.class
        );
        PrintReflection.print(responseDTO);
    }
}
