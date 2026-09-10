package com.evolution.dropfile.store.share;

import lombok.With;

import java.time.Instant;

@With
public record ShareFile(String alias,
                        String resourcePath,
                        String hash,
                        long size,
                        boolean accessible,
                        Instant fileLastModified,
                        Instant created) {
}
