package com.evolution.dropfile.store.access;

import java.time.Instant;

public record AccessKey(String id,
                        String key,
                        Instant created) {
}
