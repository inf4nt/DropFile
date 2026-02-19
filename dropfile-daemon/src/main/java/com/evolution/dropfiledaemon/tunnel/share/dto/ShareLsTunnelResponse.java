package com.evolution.dropfiledaemon.tunnel.share.dto;

import java.time.Instant;

public record ShareLsTunnelResponse(String id,
                                    String alias,
                                    long size,
                                    Instant created) {
}
