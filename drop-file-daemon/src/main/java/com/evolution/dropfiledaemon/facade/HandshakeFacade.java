package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.configuration.CommonUtils;
import com.evolution.dropfile.configuration.dto.HandshakeApiRequestDTO;
import com.evolution.dropfile.configuration.dto.HandshakeRequestDTO;
import com.evolution.dropfiledaemon.client.HandshakeClient;
import com.evolution.dropfiledaemon.configuration.DropFileKeyPairProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpResponse;
import java.security.KeyPair;
import java.security.PublicKey;

@Component
public class HandshakeFacade {

    private final HandshakeClient handshakeClient;

    private final DropFileKeyPairProvider keyPairProvider;

    private final ObjectMapper objectMapper;

    private final Environment environment;

    @Autowired
    public HandshakeFacade(HandshakeClient handshakeClient,
                           DropFileKeyPairProvider keyPairProvider,
                           ObjectMapper objectMapper, Environment environment) {
        this.handshakeClient = handshakeClient;
        this.keyPairProvider = keyPairProvider;
        this.objectMapper = objectMapper;
        this.environment = environment;
    }

    @SneakyThrows
    public void handshakeRequest(HandshakeApiRequestDTO handShakeApiRequestDTO) {
        Integer port = Integer.valueOf(environment.getProperty("server.port", "8080"));

        KeyPair keyPair = keyPairProvider.getKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        HandshakeRequestDTO handshakeRequestDTO = new HandshakeRequestDTO(publicKey.getEncoded(), port);
        byte[] handshakeRequestPayload = objectMapper.writeValueAsBytes(handshakeRequestDTO);
        String nodeAddress = handShakeApiRequestDTO.getNodeAddress();
        URI nodeAddressURI = CommonUtils.toURI(nodeAddress);
        HttpResponse<byte[]> httpResponse = handshakeClient.handshakeRequest(
                nodeAddressURI,
                handshakeRequestPayload
        );
        if (httpResponse.statusCode() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + httpResponse.statusCode());
        }

//        HandshakeRequestResponseDTO handshakeRequestResponseDTO = objectMapper
//                .readValue(httpResponse.body(), HandshakeRequestResponseDTO.class);
//
//        byte[] encryptMessage = handshakeRequestResponseDTO.encryptMessage();
//        byte[] decryptMessage = CryptoUtils.decrypt(keyPair.getPrivate(), encryptMessage);
//        String message = new String(decryptMessage);
//
//        String fingerPrint = CryptoUtils.getFingerPrint(handshakeRequestResponseDTO.publicKey());
//
//        System.out.println("Message : " + message);
//        System.out.println("FingerPrint : " + fingerPrint);
    }
}
