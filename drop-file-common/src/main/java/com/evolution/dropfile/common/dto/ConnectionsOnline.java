package com.evolution.dropfile.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionsOnline {

    private List<Online> onlineList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Online {
        private OnlineStatus status;
        private String connectionId;
        private String connectionAddress;
    }

    public enum OnlineStatus {
        ONLINE,
        OFFLINE
    }
}
