package com.evolution.dropfiledaemon.tunnel.framework;

public interface TunnelClient {

    <T> T send(Request request, Class<T> responseType);

    record Request(String action,
                   String fingerprint,
                   Object payload) {
        public Request(String action, String fingerprint, Object payload) {
            this.action = action;
            this.fingerprint = fingerprint;
            this.payload = payload;
        }

        public Request(String action, String fingerprint) {
            this(action, fingerprint, null);
        }
    }
}
