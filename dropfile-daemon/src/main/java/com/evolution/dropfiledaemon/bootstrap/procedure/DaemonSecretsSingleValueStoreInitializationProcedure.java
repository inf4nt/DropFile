package com.evolution.dropfiledaemon.bootstrap.procedure;

import com.evolution.dropfile.common.crypto.CryptoRSA;
import com.evolution.dropfile.store.framework.single.SingleValueStoreInitializationProcedure;
import com.evolution.dropfile.store.secret.DaemonSecret;
import com.evolution.dropfile.store.secret.DaemonSecretStore;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DaemonSecretsSingleValueStoreInitializationProcedure
        implements SingleValueStoreInitializationProcedure {

    private final DaemonSecretStore store;

    // TODO fix it @SneakyThrows
    @SneakyThrows
    @Override
    public void init() {
        Optional<DaemonSecret> daemonSecretOptional = store.get();
        if (daemonSecretOptional.isEmpty()) {
            log.info("No daemon secret found. Initializing");
            KeyPair keyPair = CryptoRSA.generateKeyPair();
            store.save(new DaemonSecret(
                    UUID.randomUUID(),
                    keyPair.getPublic().getEncoded(),
                    keyPair.getPrivate().getEncoded()
            ));
        } else {
            DaemonSecret daemonSecretUpdated = daemonSecretOptional.get().withDaemonToken(UUID.randomUUID());
            store.save(daemonSecretUpdated);
        }
    }
}
