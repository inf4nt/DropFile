package com.evolution.dropfiledaemon.security;

import com.evolution.dropfile.store.secret.DaemonSecretsStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@RequiredArgsConstructor
@Slf4j
@Service
public class TokenService {

    private final DaemonSecretsStore daemonSecretsStore;

    public boolean isValid(String token) {
        try {
            if (ObjectUtils.isEmpty(token) || token.trim().isEmpty()) {
                return false;
            }
            String daemonToken = daemonSecretsStore.getRequired().daemonToken();
            return daemonToken.equals(token);
        } catch (Exception exception) {
            log.info("Token validation failed message {}", exception.getMessage(), exception);
            return false;
        }
    }
}

