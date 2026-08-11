package com.evolution.dropfile.store.quickshare;

import lombok.With;

import java.time.Instant;

@With
public record QuickShareEntry(String resourcePath,
                              String secret,
                              boolean directory,
                              boolean singleUse,
                              boolean secure,
                              boolean expired,
                              Instant updated,
                              Instant created) {
}
