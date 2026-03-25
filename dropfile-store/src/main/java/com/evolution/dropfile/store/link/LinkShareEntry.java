package com.evolution.dropfile.store.link;

import lombok.With;

import java.time.Instant;

@With
public record LinkShareEntry(String fileId,
                             String secret,
                             boolean used,
                             Instant updated,
                             Instant created) {
}
