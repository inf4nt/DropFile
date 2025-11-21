package com.evolution.dropfiledaemon.security;

import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final String token;

    @SneakyThrows
    public TokenService() {
        this.token = "dropfile_test_token";
        System.out.println("Token " + this.token);
    }

    public boolean isValid(String tokenIncoming) {
        return token.equals(tokenIncoming);
    }
}

