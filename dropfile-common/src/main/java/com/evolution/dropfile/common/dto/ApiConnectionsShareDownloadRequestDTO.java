package com.evolution.dropfile.common.dto;

import java.util.List;

public record ApiConnectionsShareDownloadRequestDTO(List<DownloadItem> downloadItems,
                                                    boolean force) {

    public record DownloadItem(String id, String filename) {

    }
}
