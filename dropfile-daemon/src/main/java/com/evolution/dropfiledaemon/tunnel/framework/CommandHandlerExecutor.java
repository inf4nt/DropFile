package com.evolution.dropfiledaemon.tunnel.framework;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CommandHandlerExecutor {

    private final Map<String, CommandHandler> handers;

    private final ObjectMapper objectMapper;

    public CommandHandlerExecutor(List<CommandHandler> handers,
                                  ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        Map<String, CommandHandler> map = new HashMap<>();
        for (CommandHandler commandHandler : handers) {
            if (map.containsKey(commandHandler.getCommandName())) {
                throw new RuntimeException("Duplicate : " + commandHandler.getCommandName());
            }
            map.put(commandHandler.getCommandName(), commandHandler);
        }
        this.handers = map;
    }

    @SneakyThrows
    public Object handle(TunnelRequestDTO.TunnelRequestPayload payload) {
        CommandHandler commandHandler = getHandler(payload.command());
        if (commandHandler == null) {
            throw new RuntimeException("No found: " + payload.command());
        }
        Object handlerArgumentPayload = getBody(commandHandler, payload);
        return commandHandler.handle(handlerArgumentPayload);
    }

    @SneakyThrows
    private Object getBody(CommandHandler commandHandler, TunnelRequestDTO.TunnelRequestPayload payload) {
        Object body;
        if (commandHandler.getPayloadType().equals(Void.class)) {
            body = null;
        } else if (commandHandler.getPayloadType().equals(String.class)) {
            body = new String(payload.payload(), StandardCharsets.UTF_8);
        } else if (commandHandler.getPayloadType().equals(byte[].class)) {
            body = payload.payload();
        } else {
            body = objectMapper.readValue(payload.payload(), commandHandler.getPayloadType());
        }
        return body;
    }

    private CommandHandler getHandler(String command) {
        CommandHandler commandHandler = handers.get(command);
        if (commandHandler == null) {
            throw new RuntimeException("No handler found: " + command);
        }
        return commandHandler;
    }
}
