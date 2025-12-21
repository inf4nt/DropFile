package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoUtils;
import com.evolution.dropfile.common.dto.DaemonInfoResponseDTO;
import com.evolution.dropfile.common.dto.DaemonSetPublicAddressRequestBodyDTO;
import com.evolution.dropfile.configuration.app.DropFileAppConfig;
import com.evolution.dropfile.configuration.app.DropFileAppConfigStore;
import com.evolution.dropfile.configuration.keys.DropFileKeysConfigStore;
import com.evolution.dropfiledaemon.client.NodeClient;
import com.evolution.dropfiledaemon.exception.ApiFacadePingNodeException;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStore;
import com.evolution.dropfiledaemon.handshake.store.TrustedOutKeyValueStore;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;

@Component
public class ApiFacade {

    private final NodeClient nodeClient;

    private final HandshakeStore handshakeStore;

    private final DropFileAppConfigStore appConfigStore;

    private final DropFileKeysConfigStore keysConfigStore;

    @Autowired
    public ApiFacade(NodeClient nodeClient,
                     HandshakeStore handshakeStore,
                     DropFileAppConfigStore appConfigStore,
                     DropFileKeysConfigStore keysConfigStore) {
        this.nodeClient = nodeClient;
        this.handshakeStore = handshakeStore;
        this.appConfigStore = appConfigStore;
        this.keysConfigStore = keysConfigStore;
    }


    public void setPublicAddress(DaemonSetPublicAddressRequestBodyDTO requestBodyDTO) {
        DropFileAppConfig existingAppConfig = appConfigStore.getRequired();

        DropFileAppConfig newOne = new DropFileAppConfig(
                existingAppConfig.cliAppConfig(),
                new DropFileAppConfig.DropFileDaemonAppConfig(
                        existingAppConfig.daemonAppConfig().downloadDirectory(),
                        existingAppConfig.daemonAppConfig().daemonPort(),
                        CommonUtils.toURI(requestBodyDTO.address())
                )
        );
        appConfigStore.save(newOne);
    }

    public void shutdown() {
        Executors.newSingleThreadExecutor()
                .submit(() -> {
                    System.exit(0);
                });
    }

    public DaemonInfoResponseDTO getDaemonInfo() {
        byte[] publicKey = keysConfigStore.getRequired().publicKey();
        String publicKeyBase64 = CryptoUtils.encodeBase64(publicKey);
        String fingerPrint = CryptoUtils.getFingerPrint(publicKey);

        return new DaemonInfoResponseDTO(fingerPrint, publicKeyBase64);
    }

    @SneakyThrows
    public String nodePing(String fingerprint) {
        TrustedOutKeyValueStore.TrustedOutValue trustedOutValue = handshakeStore
                .trustedOutStore()
                .get(fingerprint)
                .orElseThrow();
        byte[] encrypt = CryptoUtils.encrypt(trustedOutValue.publicKey(), trustedOutValue.secret().getBytes());
        String tokenBase64 = CryptoUtils.encodeBase64(encrypt);

        URI nodeAddressURI = trustedOutValue.addressURI();

        HttpResponse<String> httpResponse = nodeClient.nodePing(nodeAddressURI, tokenBase64);
        if (httpResponse.statusCode() == 200) {
            return httpResponse.body();
        }
        throw new ApiFacadePingNodeException(fingerprint);
    }
}
