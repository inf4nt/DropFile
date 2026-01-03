package com.evolution.dropfile.configuration.access;

import java.time.Instant;

public record AccessKey(String id,
                        String key,
                        Instant created) {
}
