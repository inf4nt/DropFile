package com.evolution.dropfiledaemon.tunnel.framework.client;

import com.evolution.dropfiledaemon.tunnel.framework.exception.TunnelClientException;
import lombok.Builder;
import lombok.Getter;

import java.io.InputStream;

public interface TunnelClient {

    InputStream stream(Request request) throws TunnelClientException;

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
