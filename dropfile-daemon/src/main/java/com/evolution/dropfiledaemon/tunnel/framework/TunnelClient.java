package com.evolution.dropfiledaemon.tunnel.framework;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Builder;
import lombok.Getter;

import java.io.InputStream;
import java.lang.reflect.Type;

public interface TunnelClient {

    default InputStream stream(Request request) {
        return send(request, InputStream.class);
    }

    default <T> T send(Request request, Class<T> responseType) {
        return send(request, new TypeReference<T>() {
            @Override
            public Type getType() {
                return responseType;
            }
        });
    }

    <T> T send(Request request, TypeReference<T> responseType);

    @Builder
    @Getter
    class Request {
        private final String action;
        private final String fingerprint;
        private final Object body;
    }
}
