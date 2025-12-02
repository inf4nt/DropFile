package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.configuration.CommonUtils;
import com.evolution.dropfile.configuration.crypto.CryptoUtils;
import com.evolution.dropfile.configuration.dto.HandshakeRequestApprovedDTO;
import com.evolution.dropfile.configuration.dto.HandshakeRequestDTO;
import com.evolution.dropfile.configuration.keys.DropFileKeysConfigManager;
import com.evolution.dropfiledaemon.client.HandshakeClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpResponse;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/handshake")
public class HandshakeRestController {

    private final Map<String, byte[]> publicKeys = new HashMap<>();

    private final Map<String, URI> fingerPrintRequests = new HashMap<>();

    private final Map<String, URI> fingerPrintRequestsTrusted = new HashMap<>();

    private final Map<String, Object> fingerPrintRequestsApproved = new HashMap<>();

    @Autowired
    private HandshakeClient handshakeClient;

    @Autowired
    private DropFileKeysConfigManager keysConfigManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Environment environment;

    @SneakyThrows
    @PostMapping("/request")
    public ResponseEntity<Void> handshakeRequest(@RequestBody HandshakeRequestDTO handShakeRequestDTO,
                                                 HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        URI requestAddressURI = CommonUtils.toURI(remoteAddr + ":" + handShakeRequestDTO.getPort());
        String fingerPrint = CryptoUtils.getFingerPrint(handShakeRequestDTO.getPublicKey());
        fingerPrintRequests.put(fingerPrint, requestAddressURI);
        publicKeys.put(fingerPrint, handShakeRequestDTO.getPublicKey());

        return ResponseEntity.ok().build();
    }

    @GetMapping
    public Map getRequests() {
        return Map.of(
                "fingerPrintRequests", fingerPrintRequests,
                "fingerPrintRequestsTrusted", fingerPrintRequestsTrusted,
                "fingerPrintRequestsApproved", fingerPrintRequestsApproved
        );
    }

    @SneakyThrows
    @PostMapping("/request/approve/{fingerprint}")
    public void requestApprove(@PathVariable String fingerprint) {
        URI nodeURI = fingerPrintRequests.get(fingerprint);
        if (nodeURI == null) {
            throw new BadRequestException("Fingerprint not found");
        }

        PublicKey publicKeyNode = CryptoUtils.getPublicKey(publicKeys.get(fingerprint));
        String secret = UUID.randomUUID().toString();
        System.out.println("Secret: " + secret);
        byte[] encryptSecret = CryptoUtils.encrypt(publicKeyNode, secret.getBytes());

        PublicKey myPublicKey = keysConfigManager.getKeyPair().getPublic();

        Integer port = Integer.valueOf(environment.getProperty("server.port", "8080"));
        HandshakeRequestApprovedDTO handshakeRequestApprovedDTO = new HandshakeRequestApprovedDTO(
                myPublicKey.getEncoded(),
                port,
                encryptSecret
        );
        byte[] payload = objectMapper.writeValueAsBytes(handshakeRequestApprovedDTO);

        HttpResponse<byte[]> httpResponse = handshakeClient.handshakeRequestApproved(nodeURI, payload);
        if (httpResponse.statusCode() == 200) {
            fingerPrintRequests.remove(fingerprint);
            fingerPrintRequestsTrusted.put(fingerprint, nodeURI);
        } else {
            throw new RuntimeException();
        }
    }

    @SneakyThrows
    @PostMapping("/request/approved")
    public void requestApproved(@RequestBody HandshakeRequestApprovedDTO request,
                                HttpServletRequest httpServletRequest) {
        String remoteAddr = httpServletRequest.getRemoteAddr();
        URI nodeAddressURI = CommonUtils.toURI(remoteAddr + ":" + request.port());

        PrivateKey privateKey = keysConfigManager.getKeyPair().getPrivate();
        byte[] decryptMessage = CryptoUtils.decrypt(privateKey, request.encryptMessage());
        String message = new String(decryptMessage);
        System.out.println("Message: " + message);

        String fingerPrint = CryptoUtils.getFingerPrint(request.publicKey());

        fingerPrintRequestsApproved.put(fingerPrint, nodeAddressURI);
    }
}
