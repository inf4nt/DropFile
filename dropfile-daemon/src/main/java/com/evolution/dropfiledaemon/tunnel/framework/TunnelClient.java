package com.evolution.dropfiledaemon.tunnel.framework;

public interface TunnelClient {

    <T> T send(Request request, Class<T> responseType);

    record Request(String action,
                   String fingerprint,
                   Object payload) {
        public Request(String action) {
            this(action, null, null);
        }

        public Request(String action, Object payload) {
            this(action, null, payload);
        }
    }
}
