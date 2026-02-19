package com.evolution.dropfile.store.share;

import java.time.Instant;

public record ShareFileEntry(String alias,
                             String absolutePath,
                             long size,
                             Instant created) {
}
