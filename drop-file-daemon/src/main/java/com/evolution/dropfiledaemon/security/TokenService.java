package com.evolution.dropfiledaemon.security;

import com.evolution.dropfile.configuration.secret.DropFileSecretsConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final ObjectProvider<DropFileSecretsConfig> secretsConfigObjectProvider;

    @Autowired
    public TokenService(ObjectProvider<DropFileSecretsConfig> secretsConfigObjectProvider) {
        this.secretsConfigObjectProvider = secretsConfigObjectProvider;
    }

    public boolean isValid(String tokenIncoming) {
        String daemonToken = secretsConfigObjectProvider.getObject().getDaemonToken();
        return daemonToken.equals(tokenIncoming);
    }
}

