package com.evolution.dropfiledaemon.tunnel.handler;

import com.evolution.dropfiledaemon.tunnel.TunnelRequestDTO;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DefaultTunnelActionHandler implements TunnelActionHandler {

    private final Map<String, ActionHandler> handlerActions;

    @Autowired
    public DefaultTunnelActionHandler(List<ActionHandler> actionHandlers) {
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
        return actionHandler.handle(payload);
    }
}
