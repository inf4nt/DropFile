package com.evolution.dropfilecli.command.request;

import com.evolution.dropfile.common.dto.HandshakeApiIncomingResponseDTO;
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
        name = "incoming",
        description = "Retrieve incoming connection requests"
)
public class IncomingRequestConnectionCommand implements CommandHttpHandler<byte[]> {

    private final DaemonClient daemonClient;

    private final ObjectMapper objectMapper;

    public IncomingRequestConnectionCommand(DaemonClient daemonClient,
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
            for (int i = 0; i < values.size(); i++) {
                if (i ==0) {
                    System.out.println("---------------------------");
                }
                HandshakeApiIncomingResponseDTO value = values.get(i);
                System.out.println("Fingerprint: " + value.fingerPrint());
                System.out.println("PublicKey: " + value.publicKey());
                System.out.println("AddressURI: " + value.addressURI());
                if (i <= values.size() - 1) {
                    System.out.println("---------------------------");
                }
            }
        } else {
            System.out.println("No incoming requests found");
        }
    }
}
