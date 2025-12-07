package com.evolution.dropfilecli.command.peer;

import com.evolution.dropfile.common.dto.HandshakeApiTrustOutResponseDTO;
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
        name = "trusted-out",
        description = "Retrieve trusted-out connections"
)
public class PeersConnectionTrustedOutCommand implements CommandHttpHandler<byte[]> {

    private final DaemonClient daemonClient;

    private final ObjectMapper objectMapper;

    @Autowired
    public PeersConnectionTrustedOutCommand(DaemonClient daemonClient,
                                            ObjectMapper objectMapper) {
        this.daemonClient = daemonClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.getTrustOut();
    }

    @Override
    public void handleSuccessful(HttpResponse<byte[]> response) throws Exception {
        List<HandshakeApiTrustOutResponseDTO> values = objectMapper
                .readValue(
                        response.body(),
                        new TypeReference<List<HandshakeApiTrustOutResponseDTO>>() {
                        }
                );
        if (!values.isEmpty()) {
            for (int i = 0; i < values.size(); i++) {
                if (i ==0) {
                    System.out.println("---------------------------");
                }
                HandshakeApiTrustOutResponseDTO value = values.get(i);
                System.out.println("Fingerprint: " + value.fingerPrint());
                System.out.println("PublicKey: " + value.publicKey());
                System.out.println("AddressURI: " + value.addressURI());
                if (i <= values.size() - 1) {
                    System.out.println("---------------------------");
                }
            }
        } else {
            System.out.println("No trusted-out connections found");
        }
    }
}
