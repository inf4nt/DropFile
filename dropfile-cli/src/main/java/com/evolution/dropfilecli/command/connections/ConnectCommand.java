package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfile.common.crypto.CryptoUtils;
import com.evolution.dropfile.common.dto.HandshakeApiRequestBodyDTO;
import com.evolution.dropfile.common.dto.HandshakeApiRequestResponseStatus;
import com.evolution.dropfile.common.dto.HandshakeIdentityResponseDTO;
import com.evolution.dropfilecli.client.DaemonClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "connect",
        description = "Connect"
)
public class ConnectCommand implements Runnable {

    @CommandLine.Parameters(index = "0", description = "<host>:<port>")
    private String address;

    @CommandLine.Parameters(index = "1", description = "Timeout in seconds. Default value 60sec", defaultValue = "60")
    private Integer timeout;

    private final DaemonClient daemonClient;

    private final ObjectMapper objectMapper;

    @Autowired
    public ConnectCommand(DaemonClient daemonClient,
                          ObjectMapper objectMapper) {
        this.daemonClient = daemonClient;
        this.objectMapper = objectMapper;
    }

    @SneakyThrows
    @Override
    public void run() {
        HandshakeIdentityResponseDTO identity = getIdentity(address);
        String publicKeyBase64 = identity.payload().publicKeyRSA();
        String fingerprint = CryptoUtils.getFingerprint(CryptoUtils.decodeBase64(publicKeyBase64));

        System.out.println("RSA fingerprint is: " + fingerprint);

        HttpResponse<byte[]> trustOut = daemonClient.getTrustOut(fingerprint);
        if (trustOut.statusCode() == 200) {
            System.out.println("Reconnecting...");
            handshakeRequest(identity.payload());
            return;
        }

        HttpResponse<byte[]> outgoingRequestResponse = daemonClient.getOutgoingRequest(fingerprint);
        if (outgoingRequestResponse.statusCode() != 200 && !confirmationEstablishConnection()) {
            return;
        }

        handshakeRequest(identity.payload());
    }

    @SneakyThrows
    private HandshakeIdentityResponseDTO getIdentity(String address) {
        HttpResponse<byte[]> identityResponse = daemonClient.getHandshakeIdentity(address);
        if (identityResponse.statusCode() != 200) {
            throw new RuntimeException("Identity request failed: " + identityResponse.statusCode());
        }
        HandshakeIdentityResponseDTO responseDTO = objectMapper
                .readValue(identityResponse.body(), HandshakeIdentityResponseDTO.class);
        byte[] payloadBytesExpected = objectMapper.writeValueAsBytes(responseDTO.payload());

        boolean verify = CryptoUtils.verify(
                payloadBytesExpected,
                CryptoUtils.decodeBase64(responseDTO.signature()),
                CryptoUtils.getPublicKey(CryptoUtils.decodeBase64(responseDTO.payload().publicKeyRSA()))
        );

        if (!verify) {
            throw new RuntimeException("Verification failed");
        }

        return responseDTO;
    }

    @SneakyThrows
    private void handshakeRequest(HandshakeIdentityResponseDTO.HandshakeIdentityPayload identityPayload) {
        int count = 1;
        HttpResponse<String> retry;
        while (count <= timeout) {
            System.out.println(String.format("Attempt %s to connect %s", count, address));

            HandshakeApiRequestBodyDTO requestBodyDTO = new HandshakeApiRequestBodyDTO(
                    address,
                    identityPayload.publicKeyRSA(),
                    identityPayload.publicKeyDH()
            );

            retry = daemonClient.handshakeRequest(requestBodyDTO);
            if (retry.statusCode() != 200) {
                System.out.println("Handshake request failed");
            }
            System.out.println("Handshake status: " + retry.statusCode());
            System.out.println("Handshake response: " + retry.body());
            if (HandshakeApiRequestResponseStatus.SUCCESS.name().equals(retry.body())) {
                break;
            }
            count++;
            Thread.sleep(1000);
        }
    }

    private boolean confirmationEstablishConnection() {
        System.out.println(String.format("The authenticity of host %s cannot be established.", address));
        while (true) {
            String answer = System.console().readLine("Are you sure you want continue connecting (yes/no): ");
            if (answer.equalsIgnoreCase("yes")) {
                return true;
            }
            if (answer.equalsIgnoreCase("no")) {
                return false;
            }
        }
    }
}
