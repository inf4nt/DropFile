package com.evolution.dropfiledaemon.security;

import com.evolution.dropfile.common.crypto.CryptoUtils;
import com.evolution.dropfile.configuration.keys.DropFileKeysConfig;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStore;
import com.evolution.dropfiledaemon.handshake.store.TrustedInKeyValueStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class HandshakeSecretTokenService {

    private final HandshakeStore handshakeStore;

    private final DropFileKeysConfig keysConfig;

    public boolean isValid(String tokenBase64) {
        if (tokenBase64 == null) {
            return false;
        }

        Map<String, TrustedInKeyValueStore.TrustedInValue> trusted = handshakeStore
                .trustedInStore()
                .getAll();
        if (trusted.isEmpty()) {
            return false;
        }

        byte[] token = CryptoUtils.decodeBase64(tokenBase64);
        byte[] decryptTokenSecret = CryptoUtils.decrypt(keysConfig.getKeyPair().getPrivate(), token);
        String tokenSecret = new String(decryptTokenSecret);

        System.out.println("Given token secret: " + tokenSecret);
        return trusted.values()
                .stream()
                .anyMatch(it -> it.secret().equals(tokenSecret));
    }
}
