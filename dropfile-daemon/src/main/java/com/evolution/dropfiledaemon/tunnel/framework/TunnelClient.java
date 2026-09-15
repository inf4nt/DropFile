package com.evolution.dropfiledaemon.tunnel.framework;

import com.evolution.dropfile.common.Attributes;
import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public interface TunnelClient {

    InputStream stream(Request request) throws IOException;

    @Builder
    @Getter
    class Request {

        private final String command;

        private final String fingerprint;

        @Nullable
        private final Object body;

        private final Attributes attributes;

        public static RequestBuilder builder(String command, String fingerprint) {
            return builder(command, fingerprint, new Attributes());
        }

        public static RequestBuilder builder(String command, String fingerprint, Attributes attributes) {
            return new RequestBuilder()
                    .command(Objects.requireNonNull(command))
                    .fingerprint(Objects.requireNonNull(fingerprint))
                    .attributes(Objects.requireNonNull(attributes));
        }

    }
}
