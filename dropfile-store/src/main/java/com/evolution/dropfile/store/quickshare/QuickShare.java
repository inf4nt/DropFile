package com.evolution.dropfile.store.quickshare;

import lombok.With;

import java.time.Instant;

@With
public record QuickShare(String resourcePath,
                         String secret,
                         boolean directory,
                         boolean singleUse,
                         boolean secure,
                         boolean expired,
                         Instant updated,
                         Instant created) {
}
