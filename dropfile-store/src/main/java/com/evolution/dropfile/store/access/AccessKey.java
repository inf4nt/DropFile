package com.evolution.dropfile.store.access;

import java.time.Duration;
import java.time.Instant;

public record AccessKey(String key,
                        Duration ttl,
                        Instant created) {
}
