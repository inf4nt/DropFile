package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoUtils;
import com.evolution.dropfile.common.dto.DaemonInfoResponseDTO;
import com.evolution.dropfile.common.dto.DaemonSetPublicAddressRequestBodyDTO;
import com.evolution.dropfile.configuration.app.AppConfig;
import com.evolution.dropfile.configuration.app.AppConfigStore;
import com.evolution.dropfile.configuration.keys.KeysConfigStore;
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

    private final AppConfigStore appConfigStore;

    private final KeysConfigStore keysConfigStore;

    @Autowired
    public ApiFacade(NodeClient nodeClient,
                     HandshakeStore handshakeStore,
                     AppConfigStore appConfigStore,
                     KeysConfigStore keysConfigStore) {
        this.nodeClient = nodeClient;
        this.handshakeStore = handshakeStore;
        this.appConfigStore = appConfigStore;
        this.keysConfigStore = keysConfigStore;
    }


    public void setPublicAddress(DaemonSetPublicAddressRequestBodyDTO requestBodyDTO) {
        AppConfig existingAppConfig = appConfigStore.getRequired();

        AppConfig newOne = new AppConfig(
                existingAppConfig.cliAppConfig(),
                new AppConfig.DaemonAppConfig(
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
        String fingerprint = CryptoUtils.getFingerprint(publicKey);

        return new DaemonInfoResponseDTO(fingerprint, publicKeyBase64);
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
