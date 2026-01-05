package com.evolution.dropfile.store.keys;

import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.common.crypto.CryptoRSA;
import com.evolution.dropfile.store.store.single.StoreInitializationProcedure;

import java.security.KeyPair;
import java.util.Optional;

public class KeysConfigStoreInitializationProcedure
        implements StoreInitializationProcedure<KeysConfigStore> {
    @Override
    public void init(KeysConfigStore store) {
        Optional<KeysConfig> configOptional = store.get();
        if (configOptional.isPresent()) {
            return;
        }

        KeyPair keyPairRSA = CryptoRSA.generateKeyPair();
        KeyPair keyPairDH = CryptoECDH.generateKeyPair();

        KeysConfig config = new KeysConfig(
                new KeysConfig.Keys(
                        keyPairRSA.getPublic().getEncoded(),
                        keyPairRSA.getPrivate().getEncoded()
                ),
                new KeysConfig.Keys(
                        keyPairDH.getPublic().getEncoded(),
                        keyPairDH.getPrivate().getEncoded()
                )
        );
        store.save(config);
    }
}
