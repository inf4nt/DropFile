package com.evolution.dropfile.store.access;

import java.time.Instant;

public record AccessKey(String key,
                        Instant created) {
}
