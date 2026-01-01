package com.evolution.dropfiledaemon.tunnel;

import java.net.URI;

public interface TunnelClient {

    <T> T send(Request request, Class<T> responseType);

    <T> T send(RequestTrusted request, Class<T> responseType);

    interface Request {

        URI getAddress();

        String getAction();

        Object getPayload();

        String getPublicKeyDH();
    }

    interface RequestEmptyBody extends Request {
        @Override
        default Object getPayload() {
            return null;
        }
    }

    interface RequestTrusted {

        String fingerprint();

        String getAction();

        Object getPayload();
    }

    interface RequestTrustedEmptyBody extends RequestTrusted {
        @Override
        default Object getPayload() {
            return null;
        }
    }
}
