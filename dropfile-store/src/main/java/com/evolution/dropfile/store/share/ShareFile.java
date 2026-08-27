package com.evolution.dropfile.store.share;

import java.time.Instant;

public record ShareFile(String alias,
                        String resourcePath,
                        long size,
                        Instant created) {
}
