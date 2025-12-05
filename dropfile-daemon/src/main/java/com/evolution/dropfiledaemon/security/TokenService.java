package com.evolution.dropfiledaemon.security;

import com.evolution.dropfile.configuration.secret.DropFileSecretsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.Objects;

@Service
public class TokenService {

    private final DropFileSecretsConfig secretsConfig;

    @Autowired
    public TokenService(DropFileSecretsConfig secretsConfig) {
        this.secretsConfig = secretsConfig;
    }

    public boolean isValid(String tokenIncoming) {
        if (ObjectUtils.isEmpty(tokenIncoming)) {
            return false;
        }
        String daemonToken = secretsConfig.getDaemonToken();
        Objects.requireNonNull(daemonToken);
        return daemonToken.equals(tokenIncoming);
    }
}

