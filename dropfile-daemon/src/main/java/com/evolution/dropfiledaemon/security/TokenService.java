package com.evolution.dropfiledaemon.security;

import com.evolution.dropfile.store.secret.DaemonSecretStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RequiredArgsConstructor
@Slf4j
@Service
public class TokenService {

    private final DaemonSecretStore daemonSecretStore;

    public boolean isValid(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }

        try {
            String daemonToken = daemonSecretStore.getRequired().daemonToken();
            if (!StringUtils.hasText(daemonToken)) {
                return false;
            }

            return MessageDigest.isEqual(
                    daemonToken.getBytes(StandardCharsets.UTF_8),
                    token.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception exception) {
            log.error("Token validation failed due to secrets store error: {}", exception.getMessage(), exception);
            return false;
        }
    }
}
