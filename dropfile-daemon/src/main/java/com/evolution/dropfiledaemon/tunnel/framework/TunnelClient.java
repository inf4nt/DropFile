package com.evolution.dropfiledaemon.tunnel.framework;

import lombok.Builder;
import lombok.Getter;

import java.io.InputStream;

public interface TunnelClient {

    InputStream stream(Request request);

    @Builder
    @Getter
    class Request {
        private final String command;
        private final String fingerprint;
        private final Object body;

        public static RequestBuilder builder(String command, String fingerprint) {
            return new RequestBuilder().command(command).fingerprint(fingerprint);
        }
    }
}
