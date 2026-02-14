package com.evolution.dropfile.store.session;

import java.time.Instant;

public record TunnelSessionEntry(byte[] dhPublicKey,
                                 byte[] dhPrivateKey,
                                 byte[] remoteDhPublicKey,
                                 Instant created) {
}
