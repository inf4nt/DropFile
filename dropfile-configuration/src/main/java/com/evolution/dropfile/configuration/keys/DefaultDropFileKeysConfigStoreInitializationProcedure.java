package com.evolution.dropfile.configuration.keys;

import com.evolution.dropfile.common.crypto.CryptoUtils;

import java.security.KeyPair;
import java.util.Optional;

public class DefaultDropFileKeysConfigStoreInitializationProcedure
        implements DropFileKeysConfigStoreInitializationProcedure {
    @Override
    public void init(DropFileKeysConfigStore store) {
        Optional<DropFileKeysConfig> configOptional = store.get();
        if (configOptional.isPresent()) {
            return;
        }

        KeyPair keyPair = CryptoUtils.generateKeyPair();
        DropFileKeysConfig config = new DropFileKeysConfig(
                keyPair.getPublic().getEncoded(),
                keyPair.getPrivate().getEncoded()
        );
        store.save(config);
    }
}
