package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoRSA;
import com.evolution.dropfile.common.dto.DaemonInfoResponseDTO;
import com.evolution.dropfile.store.keys.KeysConfigStore;
import com.evolution.dropfiledaemon.system.SystemInfoProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;

@Component
public class ApiDaemonFacade {

    private final KeysConfigStore keysConfigStore;

    private final SystemInfoProvider systemInfoProvider;

    @Autowired
    public ApiDaemonFacade(KeysConfigStore keysConfigStore,
                           SystemInfoProvider systemInfoProvider) {
        this.keysConfigStore = keysConfigStore;
        this.systemInfoProvider = systemInfoProvider;
    }

    public void shutdown() {
        Executors.newSingleThreadExecutor().execute(() -> {
            System.exit(0);
        });
    }

    public DaemonInfoResponseDTO info() {
        return new DaemonInfoResponseDTO(
                CommonUtils.getFingerprint(CryptoRSA.getPublicKey(keysConfigStore.getRequired().rsa().publicKey())),
                CommonUtils.encodeBase64(keysConfigStore.getRequired().rsa().publicKey()),
                CommonUtils.encodeBase64(keysConfigStore.getRequired().dh().publicKey()),
                systemInfoProvider.getSystemInfoAsJson()
        );
    }
}
