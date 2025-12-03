package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.configuration.CommonUtils;
import com.evolution.dropfile.configuration.crypto.CryptoUtils;
import com.evolution.dropfile.configuration.dto.HandshakeApiRequestDTO;
import com.evolution.dropfile.configuration.dto.HandshakeDTO;
import com.evolution.dropfile.configuration.dto.HandshakeRequestApprovedDTO;
import com.evolution.dropfile.configuration.dto.HandshakeRequestDTO;
import com.evolution.dropfiledaemon.InMemoryHandshakeStore;
import com.evolution.dropfiledaemon.client.HandshakeClient;
import com.evolution.dropfiledaemon.configuration.DropFileKeyPairProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpResponse;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Optional;
import java.util.UUID;

@Component
public class HandshakeFacade {

    private final HandshakeClient handshakeClient;

    private final DropFileKeyPairProvider keyPairProvider;

    private final InMemoryHandshakeStore handshakeStore;

    private final Integer currentInstancePort;

    private final ObjectMapper objectMapper;

    @Autowired
    public HandshakeFacade(HandshakeClient handshakeClient,
                           DropFileKeyPairProvider keyPairProvider,
                           InMemoryHandshakeStore handshakeStore,
                           @Value("${server.port:8080}") Integer currentInstancePort,
                           ObjectMapper objectMapper) {
        this.handshakeClient = handshakeClient;
        this.keyPairProvider = keyPairProvider;
        this.handshakeStore = handshakeStore;
        this.currentInstancePort = currentInstancePort;
        this.objectMapper = objectMapper;
    }

    public Optional<HandshakeDTO> getHandshakeDTO(String fingerprint) {
        InMemoryHandshakeStore.HandshakeEnvelope handshakeEnvelope = handshakeStore.getTrusted().get(fingerprint);
        if (handshakeEnvelope == null) {
            return Optional.empty();
        }
        byte[] publicKey = handshakeEnvelope.publicKey();
        String secret = handshakeEnvelope.secret();
        byte[] encryptSecret = CryptoUtils.encrypt(publicKey, secret.getBytes());
        PublicKey currentPublicKey = keyPairProvider.getKeyPair().getPublic();
        return Optional.of(new HandshakeDTO(currentPublicKey.getEncoded(), encryptSecret));
    }

    @SneakyThrows
    public void initializeRequest(HandshakeApiRequestDTO requestBody) {
        URI nodeAddressURI = CommonUtils.toURI(requestBody.getNodeAddress());
        PublicKey currentPublicKey = keyPairProvider.getKeyPair().getPublic();
        String currentFingerPrint = CryptoUtils.getFingerPrint(currentPublicKey);
        HttpResponse<byte[]> handshakeResponse = handshakeClient.getHandshake(nodeAddressURI, currentFingerPrint);
        if (handshakeResponse.statusCode() == 200) {
            HandshakeDTO handshakeDTO = objectMapper.readValue(handshakeResponse.body(), HandshakeDTO.class);
            PrivateKey currentPrivateKey = keyPairProvider.getKeyPair().getPrivate();
            byte[] decryptMessage = CryptoUtils.decrypt(currentPrivateKey, handshakeDTO.encryptMessage());
            String secret = new String(decryptMessage);

            String fingerPrint = CryptoUtils.getFingerPrint(handshakeDTO.publicKey());
            handshakeStore.addTrusted(
                    fingerPrint,
                    handshakeDTO.publicKey(),
                    secret
            );
        }
        if (handshakeResponse.statusCode() == 404) {
            HttpResponse<Void> httpResponse = handshakeClient.handshakeRequest(
                    nodeAddressURI,
                    currentInstancePort,
                    currentPublicKey
            );
            if (httpResponse.statusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + httpResponse.statusCode());
            }
        }
    }

    public void processRequest(HandshakeRequestDTO requestBody,
                               String incomingRemoteAddress) {
        String fingerPrint = CryptoUtils.getFingerPrint(requestBody.getPublicKey());
        if (handshakeStore.getTrusted().containsKey(fingerPrint)
                || handshakeStore.getRequests().containsKey(fingerPrint)) {
            return;
        }

        int incomingPort = requestBody.getPort();
        URI incomingAddressURI = CommonUtils.toURI(incomingRemoteAddress, incomingPort);
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

//        byte[] encryptSecret = CryptoUtils.encrypt(request.publicKey(), secret.getBytes());
//        HttpResponse<Void> httpResponse = handshakeClient
//                .handshakeRequestApproved(request.addressURI(), publicKey, encryptSecret);
//        if (httpResponse.statusCode() != 200) {
//            System.out.println("DON'T KNOW WHAT TO DO. CALLBACK IS NOT 200");
//        }
    }

    public void finalizeApprove(HandshakeRequestApprovedDTO requestBody) {
        byte[] encryptMessage = requestBody.encryptMessage();
        PrivateKey currentNodePrivateKey = keyPairProvider.getKeyPair().getPrivate();
        byte[] decryptMessage = CryptoUtils.decrypt(currentNodePrivateKey, encryptMessage);
        String fingerPrint = CryptoUtils.getFingerPrint(requestBody.publicKey());
        handshakeStore.addTrusted(fingerPrint, requestBody.publicKey(), new String(decryptMessage));
    }
}
