package com.evolution.dropfiledaemon.security;

import com.evolution.dropfile.store.secret.DaemonSecret;
import com.evolution.dropfile.store.secret.DaemonSecretStore;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class TokenService {

    private static final String BEARER_PREFIX = "Bearer ";

    private static final int UUID_STRING_LENGTH = 36;

    private static final int BEARER_HEADER_LENGTH = BEARER_PREFIX.length() + UUID_STRING_LENGTH;

    private final DaemonSecretStore daemonSecretStore;

    public boolean isValid(@Nullable UUID token) {
        if (token == null) {
            return false;
        }

        try {
            DaemonSecret secret = daemonSecretStore.getRequired();

            return token.equals(secret.daemonToken());
        } catch (Exception e) {
            log.error("Token validation failed due to: {}", e.getMessage(), e);
            return false;
        }
    }

    @Nullable
    public UUID extractToken(@Nullable HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || header.length() != BEARER_HEADER_LENGTH || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }

        String rawToken = header.substring(BEARER_PREFIX.length());
        try {
            return UUID.fromString(rawToken);
        } catch (Exception e) {
            return null;
        }
    }
}
