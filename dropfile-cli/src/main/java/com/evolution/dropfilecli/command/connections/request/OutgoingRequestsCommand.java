package com.evolution.dropfilecli.command.connections.request;

import com.evolution.dropfile.common.PrintReflection;
import com.evolution.dropfile.common.dto.HandshakeApiOutgoingResponseDTO;
import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.util.List;

@Component
@CommandLine.Command(
        name = "outgoing",
        aliases = {"--out", "-out", "--o", "-o"},
        description = "Retrieve outgoing connection requests"
)
public class OutgoingRequestsCommand implements CommandHttpHandler<byte[]> {

    private final DaemonClient daemonClient;

    private final ObjectMapper objectMapper;

    @Autowired
    public OutgoingRequestsCommand(DaemonClient daemonClient,
                                   ObjectMapper objectMapper) {
        this.daemonClient = daemonClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.getOutgoingRequests();
    }

    @Override
    public void handleSuccessful(HttpResponse<byte[]> response) throws Exception {
        List<HandshakeApiOutgoingResponseDTO> values = objectMapper
                .readValue(
                        response.body(),
                        new TypeReference<List<HandshakeApiOutgoingResponseDTO>>() {
                        }
                );
        if (!values.isEmpty()) {
            PrintReflection.print(values);
        } else {
            System.out.println("No Outgoing requests found");
        }
    }
}