package com.evolution.dropfile.common.dto;

import java.util.List;

public record LsFileTunnelResponse(List<LsFileEntry> entries) {

    public record LsFileEntry(String id,
                              String alias) {
    }
}
