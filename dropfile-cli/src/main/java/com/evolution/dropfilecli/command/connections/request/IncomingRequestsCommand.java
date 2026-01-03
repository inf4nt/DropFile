package com.evolution.dropfilecli.command.connections.request;

import com.evolution.dropfile.common.PrintReflection;
import com.evolution.dropfile.common.dto.HandshakeApiIncomingResponseDTO;
import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.util.List;

@Deprecated
@Component
@CommandLine.Command(
        name = "incoming",
        aliases = {"--in", "-in", "--i", "-i"},
        description = "Retrieve incoming connection requests"
)
public class IncomingRequestsCommand implements CommandHttpHandler<byte[]> {

    private final DaemonClient daemonClient;

    private final ObjectMapper objectMapper;

    public IncomingRequestsCommand(DaemonClient daemonClient,
                                   ObjectMapper objectMapper) {
        this.daemonClient = daemonClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.getIncomingRequests();
    }

    @Override
    public void handleSuccessful(HttpResponse<byte[]> response) throws Exception {
        List<HandshakeApiIncomingResponseDTO> values = objectMapper
                .readValue(
                        response.body(),
                        new TypeReference<List<HandshakeApiIncomingResponseDTO>>() {
                        }
                );
        if (!values.isEmpty()) {
            PrintReflection.print(values);
        } else {
            System.out.println("No incoming requests found");
        }
    }
}
