package com.evolution.dropfile.common.dto;

public record ApiDownloadLsRequest(Status status, Integer limit) {

    public enum Status {
        DOWNLOADING,
        COMPLETED,
        STOPPED,
        ERROR
    }
}
