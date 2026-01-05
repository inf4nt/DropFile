package com.evolution.dropfiledaemon.tunnel;

import com.evolution.dropfiledaemon.tunnel.framework.TunnelRequestDTO;
import com.evolution.dropfiledaemon.tunnel.framework.handler.ActionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ActionHandlerExecutor {

    private final Map<String, ActionHandler> handers;

    private final ObjectMapper objectMapper;

    public ActionHandlerExecutor(List<ActionHandler> handers,
                                 ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        Map<String, ActionHandler> map = new HashMap<>();
        for (ActionHandler actionHandler : handers) {
            if (map.containsKey(actionHandler.getAction())) {
                throw new RuntimeException("Duplicate action: " + actionHandler.getAction());
            }
            map.put(actionHandler.getAction(), actionHandler);
        }
        this.handers = map;
    }

    private ActionHandler getHandler(String action) {
        ActionHandler actionHandler = handers.get(action);
        if (actionHandler == null) {
            throw new RuntimeException("No handler found: " + action);
        }
        return actionHandler;
    }

    @SneakyThrows
    public Object handle(TunnelRequestDTO.TunnelRequestPayload payload) {
        ActionHandler actionHandler = getHandler(payload.action());
        if (actionHandler == null) {
            throw new RuntimeException("No action found: " + payload.action());
        }
        Object body;
        if (actionHandler.getPayloadType().equals(Void.class)) {
            body = null;
        } else if (actionHandler.getPayloadType().equals(String.class)) {
            body = new String(payload.payload(), StandardCharsets.UTF_8);
        } else if (actionHandler.getPayloadType().equals(byte[].class)) {
            body = payload.payload();
        } else {
            body = objectMapper.readValue(payload.payload(), actionHandler.getPayloadType());
        }
        return actionHandler.handle(body);
    }
}
