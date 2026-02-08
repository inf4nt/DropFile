package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoRSA;
import com.evolution.dropfile.common.dto.DaemonInfoResponseDTO;
import com.evolution.dropfile.store.app.AppConfigStore;
import com.evolution.dropfile.store.keys.KeysConfigStore;
import com.evolution.dropfiledaemon.system.SystemInfoProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;

@Component
public class ApiDaemonFacade {

    private final KeysConfigStore keysConfigStore;

    private final AppConfigStore appConfigStore;

    private final SystemInfoProvider systemInfoProvider;

    private final ObjectMapper objectMapper;

    @Autowired
    public ApiDaemonFacade(KeysConfigStore keysConfigStore,
                           AppConfigStore appConfigStore,
                           SystemInfoProvider systemInfoProvider,
                           ObjectMapper objectMapper) {
        this.keysConfigStore = keysConfigStore;
        this.appConfigStore = appConfigStore;
        this.systemInfoProvider = systemInfoProvider;
        this.objectMapper = objectMapper;
    }

    public void shutdown() {
        Executors.newSingleThreadExecutor().execute(() -> {
            System.exit(0);
        });
    }

    @SneakyThrows
    public DaemonInfoResponseDTO info() {
        return new DaemonInfoResponseDTO(
                CommonUtils.getFingerprint(CryptoRSA.getPublicKey(keysConfigStore.getRequired().rsa().publicKey())),
                CommonUtils.encodeBase64(keysConfigStore.getRequired().rsa().publicKey()),
                CommonUtils.encodeBase64(keysConfigStore.getRequired().dh().publicKey()),
                systemInfoProvider.getSystemInfo(),
                appConfigStore.getRequired().daemonAppConfig()
        );
    }
}
