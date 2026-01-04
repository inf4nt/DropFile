package com.evolution.dropfiledaemon.tunnel;

import com.evolution.dropfiledaemon.tunnel.framework.TunnelRequestDTO;
import com.evolution.dropfiledaemon.tunnel.framework.handler.ActionHandler;
import com.evolution.dropfiledaemon.tunnel.framework.handler.GlobalActionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MultiGlobalActionHandler implements GlobalActionHandler {

    private final Map<String, ActionHandler> handlerActions;

    private final ObjectMapper objectMapper;

    @Autowired
    public MultiGlobalActionHandler(List<ActionHandler> actionHandlers,
                                    ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        Map<String, ActionHandler> handlerActionsMap = new HashMap<>();
        for (ActionHandler actionHandler : actionHandlers) {
            String action = actionHandler.getAction();
            if (handlerActionsMap.containsKey(action)) {
                throw new RuntimeException("Duplicate action: " + action);
            }
            handlerActionsMap.put(action, actionHandler);
        }
        this.handlerActions = Collections.unmodifiableMap(handlerActionsMap);
    }

    @SneakyThrows
    @Override
    public Object handle(TunnelRequestDTO.TunnelRequestPayload payload) {
        ActionHandler actionHandler = handlerActions.get(payload.action());
        if (actionHandler == null) {
            throw new RuntimeException("No action found: " + payload.action());
        }
        if (actionHandler.getPayloadType().equals(Void.class)) {
            return actionHandler.handle(null);
        }
        Object object = objectMapper.readValue(payload.payload(), actionHandler.getPayloadType());
        return actionHandler.handle(object);
    }
}
