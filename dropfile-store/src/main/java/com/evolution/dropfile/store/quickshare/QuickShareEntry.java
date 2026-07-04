package com.evolution.dropfile.store.quickshare;

import lombok.With;

import java.time.Instant;

@With
public record QuickShareEntry(String absolutePath,
                              String alias,
                              String secret,
                              boolean singleUse,
                              boolean secure,
                              boolean expired,
                              Instant updated,
                              Instant created) {
}
