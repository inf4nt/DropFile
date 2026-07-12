package com.evolution.dropfiledaemon.tunnel.framework;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CommandHandlerExecutor {

    private final Map<String, CommandHandler> handlers;

    private final ObjectMapper objectMapper;

    public CommandHandlerExecutor(List<CommandHandler> handlersList,
                                  ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        this.handlers = handlersList.stream().collect(Collectors.toUnmodifiableMap(
                CommandHandler::getCommandName,
                Function.identity(),
                (existing, __) -> {
                    throw new IllegalStateException("Duplicate command handler found for: " + existing.getCommandName());
                }
        ));
    }

    @SneakyThrows
    public Object handle(TunnelRequestDTO.TunnelRequestPayload payload) {
        CommandHandler commandHandler = getHandler(payload.command());

        Object handlerArgumentPayload = getBody(commandHandler, payload);
        Object result = commandHandler.handle(handlerArgumentPayload);

        Objects.requireNonNull(
                result,
                String.format("Tunnel command handler '%s' returned null result", commandHandler.getCommandName())
        );

        return result;
    }

    @SneakyThrows
    private Object getBody(CommandHandler<?, ?> commandHandler, TunnelRequestDTO.TunnelRequestPayload payload) {
        Class<?> targetType = commandHandler.getPayloadType();

        if (targetType == Void.class) {
            return null;
        }
        if (targetType == String.class) {
            return new String(payload.payload(), StandardCharsets.UTF_8);
        }
        if (targetType == byte[].class) {
            return payload.payload();
        }

        return objectMapper.readValue(payload.payload(), targetType);
    }

    private CommandHandler getHandler(String command) {
        CommandHandler commandHandler = handlers.get(command);
        if (commandHandler == null) {
            throw new IllegalArgumentException("No handler found for command: " + command);
        }
        return commandHandler;
    }
}