package com.evolution.dropfiledaemon.security;

import com.evolution.dropfile.configuration.secret.DropFileSecretsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final String token;

    @Autowired
    public TokenService(DropFileSecretsConfig secretsConfig) {
        this.token = secretsConfig.getDaemonToken();
    }

    public boolean isValid(String tokenIncoming) {
        return token.equals(tokenIncoming);
    }
}

