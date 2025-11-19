package com.evolution.dropfiledaemon.node;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class NodeActiveConnections {

    private final Set<URI> connections = new LinkedHashSet<>();

    public Set<URI> getConnections() {
        return Collections.unmodifiableSet(connections);
    }

    public void addConnection(URI uri) {
        connections.add(uri);
    }
}
