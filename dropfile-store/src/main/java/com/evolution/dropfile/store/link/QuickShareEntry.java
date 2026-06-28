package com.evolution.dropfile.store.link;

import lombok.With;

import java.time.Instant;

@With
public record QuickShareEntry(String absolutePath,
                              String alias,
                              String secret,
                              boolean singleUse,
                              boolean expired,
                              Instant updated,
                              Instant created) {
}
