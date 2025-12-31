package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfile.common.PrintReflection;
import com.evolution.dropfile.common.dto.HandshakeApiTrustInResponseDTO;
import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.util.List;

@Component
@CommandLine.Command(
        name = "trusted-in",
        aliases = {"--in", "-in", "--i", "-i"},
        description = "Retrieve trusted-in connections"
)
public class TrustedInCommand implements CommandHttpHandler<byte[]> {

    private final DaemonClient daemonClient;

    private final ObjectMapper objectMapper;

    public TrustedInCommand(DaemonClient daemonClient,
                            ObjectMapper objectMapper) {
        this.daemonClient = daemonClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.getTrustIn();
    }

    @Override
    public void handleSuccessful(HttpResponse<byte[]> response) throws Exception {
        List<HandshakeApiTrustInResponseDTO> values = objectMapper
                .readValue(
                        response.body(),
                        new TypeReference<List<HandshakeApiTrustInResponseDTO>>() {
                        }
                );
        if (!values.isEmpty()) {
            PrintReflection.print(values);
        } else {
            System.out.println("No trusted-in connections found");
        }
    }
}
