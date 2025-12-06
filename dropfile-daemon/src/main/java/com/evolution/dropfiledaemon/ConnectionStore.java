package com.evolution.dropfiledaemon;

import com.evolution.dropfile.common.CommonUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ConnectionStore {

    private final Map<String, String> connections = new LinkedHashMap<>();

    public Map<String, String> getConnections() {
        return Collections.unmodifiableMap(connections);
    }

    public String addConnection(String connectionAddress) {
        if (connections.containsValue(connectionAddress)) {
            return getConnectionId(connectionAddress);
        }

        String connectionId = CommonUtils.random();
        while (connections.containsKey(connectionId)) {
            connectionId = CommonUtils.random();
        }
        connections.put(connectionId, connectionAddress);
        return connectionId;
    }

    public String getConnectionId(String connectionAddress) {
        return connections.entrySet().stream()
                .filter(it -> it.getValue().equals(connectionAddress))
                .map(it -> it.getKey())
                .findAny()
                .orElse(null);
    }
}
