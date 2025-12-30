package com.evolution.dropfilecli.command.daemon;

import com.evolution.dropfile.common.dto.DaemonInfoResponseDTO;
import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "info",
        description = "Retrieve info"
)
public class RetrieveInfoCommand implements CommandHttpHandler<byte[]> {

    private final DaemonClient daemonClient;

    private final ObjectMapper objectMapper;

    public RetrieveInfoCommand(DaemonClient daemonClient,
                               ObjectMapper objectMapper) {
        this.daemonClient = daemonClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.getDaemonInfo();
    }

    @Override
    public void handleSuccessful(HttpResponse<byte[]> response) throws Exception {
        DaemonInfoResponseDTO daemonInfoResponseDTO = objectMapper.readValue(response.body(), DaemonInfoResponseDTO.class);
        System.out.println("---------------------------");
        System.out.println("Fingerprint: " + daemonInfoResponseDTO.fingerprint());
        System.out.println("PublicKey RSA: " + daemonInfoResponseDTO.publicKeyRSA());
        System.out.println("PublicKey DH: " + daemonInfoResponseDTO.publicKeyDH());
        System.out.println("---------------------------");
    }
}
