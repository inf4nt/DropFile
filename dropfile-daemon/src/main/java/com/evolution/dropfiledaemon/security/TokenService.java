package com.evolution.dropfiledaemon.security;

import com.evolution.dropfile.store.secret.SecretsConfigStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
public class TokenService {

    private final SecretsConfigStore secretsConfigStore;

    @Autowired
    public TokenService(SecretsConfigStore secretsConfigStore) {
        this.secretsConfigStore = secretsConfigStore;
    }

    public boolean isValid(String token) {
        if (ObjectUtils.isEmpty(token) || token.trim().isEmpty()) {
            return false;
        }
        String daemonToken = secretsConfigStore.getRequired().daemonToken();
        return daemonToken.equals(token);
    }
}

