package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoUtils;
import com.evolution.dropfile.common.dto.AccessKeyGenerateRequestDTO;
import com.evolution.dropfile.common.dto.AccessKeyInfoResponseDTO;
import com.evolution.dropfile.common.dto.DaemonInfoResponseDTO;
import com.evolution.dropfile.common.dto.DaemonSetPublicAddressRequestBodyDTO;
import com.evolution.dropfile.configuration.access.AccessKey;
import com.evolution.dropfile.configuration.access.AccessKeyStore;
import com.evolution.dropfile.configuration.app.AppConfig;
import com.evolution.dropfile.configuration.app.AppConfigStore;
import com.evolution.dropfile.configuration.keys.KeysConfigStore;
import com.evolution.dropfiledaemon.client.NodeClient;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStore;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

@Component
public class ApiFacade {

    private final NodeClient nodeClient;

    private final HandshakeStore handshakeStore;

    private final AppConfigStore appConfigStore;

    private final KeysConfigStore keysConfigStore;

    private final AccessKeyStore accessKeyStore;

    @Autowired
    public ApiFacade(NodeClient nodeClient,
                     HandshakeStore handshakeStore,
                     AppConfigStore appConfigStore,
                     KeysConfigStore keysConfigStore,
                     AccessKeyStore accessKeyStore) {
        this.nodeClient = nodeClient;
        this.handshakeStore = handshakeStore;
        this.appConfigStore = appConfigStore;
        this.keysConfigStore = keysConfigStore;
        this.accessKeyStore = accessKeyStore;
    }


    @Deprecated
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
        byte[] publicKeyRSA = keysConfigStore.getRequired().rsa().publicKey();
        byte[] publicKeyDH = keysConfigStore.getRequired().dh().publicKey();
        String fingerprint = CryptoUtils.getFingerprint(publicKeyRSA);

        return new DaemonInfoResponseDTO(
                fingerprint,
                CryptoUtils.encodeBase64(publicKeyRSA),
                CryptoUtils.encodeBase64(publicKeyDH)
        );
    }

    @Deprecated
    @SneakyThrows
    public String nodePing(String fingerprint) {
//        TrustedOutKeyValueStore.TrustedOutValue trustedOutValue = handshakeStore
//                .trustedOutStore()
//                .get(fingerprint)
//                .orElseThrow();
//        byte[] encrypt = CryptoUtils.encrypt(trustedOutValue.publicKey(), trustedOutValue.secret().getBytes());
//        String tokenBase64 = CryptoUtils.encodeBase64(encrypt);
//
//        URI nodeAddressURI = trustedOutValue.addressURI();
//
//        HttpResponse<String> httpResponse = nodeClient.nodePing(nodeAddressURI, tokenBase64);
//        if (httpResponse.statusCode() == 200) {
//            return httpResponse.body();
//        }
//        throw new ApiFacadePingNodeException(fingerprint);
        throw new RuntimeException();
    }

    public List<AccessKeyInfoResponseDTO> getAccessKeys() {
        return accessKeyStore.getAll()
                .values()
                .stream()
                .map(it -> new AccessKeyInfoResponseDTO(
                        it.id(),
                        it.id() + "+" + it.key(),
                        it.created()
                ))
                .toList();
    }

    public AccessKeyInfoResponseDTO generateAccessKeys(AccessKeyGenerateRequestDTO requestDTO) {
        String id = CommonUtils.digest(CommonUtils.random().getBytes());
        String key = CommonUtils.generateSecret();

        AccessKey accessKey = accessKeyStore.save(
                id,
                new AccessKey(id, key, Instant.now())
        );

        return new AccessKeyInfoResponseDTO(
                accessKey.id(),
                accessKey.id() + "+" + accessKey.key(),
                accessKey.created()
        );
    }

    public AccessKeyInfoResponseDTO revokeAccessKey(String id) {
        AccessKey accessKey = accessKeyStore.remove(id);
        if (accessKey == null) {
            return null;
        }

        return new AccessKeyInfoResponseDTO(
                accessKey.id(),
                accessKey.id() + "+" + accessKey.key(),
                accessKey.created()
        );
    }

    public void revokeAllAccessKeys() {
        accessKeyStore.removeAll();
    }
}
