package com.evolution.dropfilecli.command.peer;

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
        aliases = {"--in", "-in"},
        description = "Retrieve trusted-in connections"
)
public class PeersConnectionTrustedInCommand implements CommandHttpHandler<byte[]> {

    private final DaemonClient daemonClient;

    private final ObjectMapper objectMapper;

    public PeersConnectionTrustedInCommand(DaemonClient daemonClient,
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
            for (int i = 0; i < values.size(); i++) {
                if (i ==0) {
                    System.out.println("---------------------------");
                }
                HandshakeApiTrustInResponseDTO value = values.get(i);
                System.out.println("Fingerprint: " + value.fingerPrint());
                System.out.println("PublicKey: " + value.publicKey());
                System.out.println("AddressURI: " + value.addressURI());
                if (i <= values.size() - 1) {
                    System.out.println("---------------------------");
                }
            }
        } else {
            System.out.println("No trusted-in connections found");
        }
    }
}
