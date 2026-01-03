package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoUtils;
import com.evolution.dropfile.common.dto.AccessKeyGenerateRequestDTO;
import com.evolution.dropfile.common.dto.AccessKeyInfoResponseDTO;
import com.evolution.dropfile.common.dto.DaemonInfoResponseDTO;
import com.evolution.dropfile.configuration.access.AccessKey;
import com.evolution.dropfile.configuration.access.AccessKeyStore;
import com.evolution.dropfile.configuration.keys.KeysConfigStore;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;

@Component
public class ApiFacade {

    private final KeysConfigStore keysConfigStore;

    private final AccessKeyStore accessKeyStore;

    public ApiFacade(KeysConfigStore keysConfigStore,
                     AccessKeyStore accessKeyStore) {
        this.keysConfigStore = keysConfigStore;
        this.accessKeyStore = accessKeyStore;
    }

    public void shutdown() {
        Executors.newSingleThreadExecutor()
                .submit(() -> {
                    System.exit(0);
                });
    }

    public DaemonInfoResponseDTO getDaemonInfo() {
        byte[] publicKeyDH = keysConfigStore.getRequired().dh().publicKey();
        return new DaemonInfoResponseDTO(
                CryptoUtils.getFingerprint(publicKeyDH),
                CryptoUtils.encodeBase64(publicKeyDH)
        );
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
