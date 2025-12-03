package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.configuration.CommonUtils;
import com.evolution.dropfile.configuration.crypto.CryptoUtils;
import com.evolution.dropfile.configuration.dto.HandshakeApiRequestDTO;
import com.evolution.dropfile.configuration.dto.HandshakeRequestApprovedDTO;
import com.evolution.dropfile.configuration.dto.HandshakeRequestDTO;
import com.evolution.dropfiledaemon.InMemoryHandshakeStore;
import com.evolution.dropfiledaemon.client.HandshakeClient;
import com.evolution.dropfiledaemon.configuration.DropFileKeyPairProvider;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpResponse;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.UUID;

@Component
public class HandshakeFacade {

    private final HandshakeClient handshakeClient;

    private final DropFileKeyPairProvider keyPairProvider;

    private final InMemoryHandshakeStore handshakeStore;

    private final Integer currentInstancePort;

    @Autowired
    public HandshakeFacade(HandshakeClient handshakeClient,
                           DropFileKeyPairProvider keyPairProvider,
                           InMemoryHandshakeStore handshakeStore,
                           @Value("${server.port:8080}") Integer currentInstancePort) {
        this.handshakeClient = handshakeClient;
        this.keyPairProvider = keyPairProvider;
        this.handshakeStore = handshakeStore;
        this.currentInstancePort = currentInstancePort;
    }


    @SneakyThrows
    public void initializeRequest(HandshakeApiRequestDTO requestBody) {
        PublicKey publicKey = keyPairProvider.getKeyPair().getPublic();
        URI nodeAddressURI = CommonUtils.toURI(requestBody.getNodeAddress());
        HttpResponse<Void> httpResponse = handshakeClient.handshakeRequest(
                nodeAddressURI,
                currentInstancePort,
                publicKey
        );
        if (httpResponse.statusCode() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + httpResponse.statusCode());
        }
    }

    public void processRequest(HandshakeRequestDTO requestBody,
                               String incomingRemoteAddress) {
        int incomingPort = requestBody.getPort();
        URI incomingAddressURI = CommonUtils.toURI(incomingRemoteAddress, incomingPort);
        String fingerPrint = CryptoUtils.getFingerPrint(requestBody.getPublicKey());
        handshakeStore.addRequest(fingerPrint, requestBody.getPublicKey(), incomingAddressURI);
    }

    public void approve(String fingerprint) {
        InMemoryHandshakeStore.HandshakeRequestEnvelope request = handshakeStore.getRequest(fingerprint);
        if (request == null) {
            throw new RuntimeException("No request found for fingerprint " + fingerprint);
        }
        PublicKey publicKey = keyPairProvider.getKeyPair().getPublic();
        String secret = UUID.randomUUID().toString();
        handshakeStore.requestToTrusted(fingerprint, secret);

        byte[] encryptSecret = CryptoUtils.encrypt(request.publicKey(), secret.getBytes());
        HttpResponse<Void> httpResponse = handshakeClient
                .handshakeRequestApproved(request.addressURI(), publicKey, encryptSecret);
        if (httpResponse.statusCode() != 200) {
            System.out.println("DON'T KNOW WHAT TO DO. CALLBACK IS NOT 200");
        }
    }

    public void finalizeApprove(HandshakeRequestApprovedDTO requestBody) {
        byte[] encryptMessage = requestBody.encryptMessage();
        PrivateKey currentNodePrivateKey = keyPairProvider.getKeyPair().getPrivate();
        byte[] decryptMessage = CryptoUtils.decrypt(currentNodePrivateKey, encryptMessage);
        String fingerPrint = CryptoUtils.getFingerPrint(requestBody.publicKey());
        handshakeStore.addTrusted(fingerPrint, requestBody.publicKey(), new String(decryptMessage));
    }
}
