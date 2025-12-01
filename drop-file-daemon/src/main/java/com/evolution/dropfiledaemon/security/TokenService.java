package com.evolution.dropfiledaemon.security;

import com.evolution.dropfile.configuration.Preconditions;
import com.evolution.dropfiledaemon.configuration.DaemonTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final DaemonTokenProvider daemonTokenProvider;

    @Autowired
    public TokenService(DaemonTokenProvider daemonTokenProvider) {
        this.daemonTokenProvider = daemonTokenProvider;
    }

    public boolean isValid(String tokenIncoming) {
        Preconditions.checkState(
                () -> !tokenIncoming.isEmpty(),
                "Given token is empty"
        );
        String daemonToken = daemonTokenProvider.getToken();
        Preconditions.checkState(
                () -> !daemonToken.isEmpty(),
                "Daemon token is empty"
        );

        return daemonToken.equals(tokenIncoming);
    }
}

