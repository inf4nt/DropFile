package com.evolution.dropfiledaemon.tunnel.framework;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Builder;
import lombok.Getter;

public interface TunnelClient {

    <T> T send(Request request, Class<T> responseType);

    <T> T send(Request request, TypeReference<T> responseType);

    @Builder
    @Getter
    class Request {
        private final String action;
        private final String fingerprint;
        private final Object body;
    }
}
