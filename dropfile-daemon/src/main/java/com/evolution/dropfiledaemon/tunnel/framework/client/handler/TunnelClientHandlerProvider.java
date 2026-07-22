package com.evolution.dropfiledaemon.tunnel.framework.client.handler;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TunnelClientHandlerProvider {

    private final Map<Integer, TunnelClientHandler> handlers;

    public TunnelClientHandlerProvider(List<TunnelClientHandler> handlers) {
        this.handlers = handlers.stream().collect(Collectors.toUnmodifiableMap(
                TunnelClientHandler::getStatusCode,
                Function.identity(),
                (existing, __) -> {
                    throw new IllegalStateException("Duplicate tunnel client handler found for status code: " + existing.getStatusCode());
                }
        ));
    }

    public TunnelClientHandler getHandler(int statusCode) {
        return Optional.ofNullable(handlers.get(statusCode))
                .orElseThrow(() -> new IllegalArgumentException("No handler found. Status code: " + statusCode));
    }
}
