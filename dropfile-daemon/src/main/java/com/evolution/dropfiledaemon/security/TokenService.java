package com.evolution.dropfiledaemon.security;

import com.evolution.dropfile.store.secret.SecretsConfigStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.Objects;

@Service
public class TokenService {

    private final SecretsConfigStore secretsConfigStore;

    @Autowired
    public TokenService(SecretsConfigStore secretsConfigStore) {
        this.secretsConfigStore = secretsConfigStore;
    }

    public boolean isValid(String tokenIncoming) {
        if (ObjectUtils.isEmpty(tokenIncoming)) {
            return false;
        }
        String daemonToken = secretsConfigStore.getRequired().daemonToken();
        Objects.requireNonNull(daemonToken);
        return daemonToken.equals(tokenIncoming);
    }
}

