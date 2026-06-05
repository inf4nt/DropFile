package com.evolution.dropfiledaemon.tunnel.command.dto;

import java.time.Instant;

public record ShareLsTunnelResponse(String id,
                                    String alias,
                                    long size,
                                    Instant created) {
}
