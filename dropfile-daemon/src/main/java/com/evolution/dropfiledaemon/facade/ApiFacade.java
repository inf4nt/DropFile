package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.crypto.CryptoUtils;
import com.evolution.dropfile.common.dto.DaemonInfoResponseDTO;
import com.evolution.dropfile.configuration.keys.DropFileKeysConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;

@Component
public class ApiFacade {

    private final DropFileKeysConfig keysConfig;

    @Autowired
    public ApiFacade(DropFileKeysConfig keysConfig) {
        this.keysConfig = keysConfig;
    }

    public void shutdown() {
        Executors.newSingleThreadExecutor()
                .submit(() -> {
                    System.exit(0);
                });
    }

    public DaemonInfoResponseDTO getDaemonInfo() {
        byte[] publicKey = keysConfig.getKeyPair().getPublic().getEncoded();
        String publicKeyBase64 = CryptoUtils.encodeBase64(publicKey);
        String fingerPrint = CryptoUtils.getFingerPrint(publicKey);

        return new DaemonInfoResponseDTO(fingerPrint, publicKeyBase64);
    }
}
