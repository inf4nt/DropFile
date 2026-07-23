package com.evolution.dropfiledaemon.tunnel.framework.server.command;

import com.evolution.dropfiledaemon.tunnel.framework.TunnelRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
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
    public Object handle(TunnelRequestDTO.Payload payload) {
        CommandHandler commandHandler = getHandler(payload.command());

        @Nullable
        Object deserializedPayload = deserialize(commandHandler.getPayloadType(), payload.payload());
        Object result = commandHandler.handle(deserializedPayload);

        Objects.requireNonNull(
                result,
                String.format("Tunnel command handler '%s' returned null result", commandHandler.getCommandName())
        );

        return result;
    }

    @Nullable
    @SneakyThrows
    private Object deserialize(Class<?> payloadType, byte[] payload) {
        if (payloadType == Void.class) {
            return null;
        }
        if (payloadType == String.class) {
            return new String(payload, StandardCharsets.UTF_8);
        }
        if (payloadType == byte[].class) {
            return payload;
        }

        return objectMapper.readValue(payload, payloadType);
    }

    private CommandHandler getHandler(String command) {
        CommandHandler commandHandler = handlers.get(command);
        if (commandHandler == null) {
            throw new IllegalArgumentException("No handler found for command: " + command);
        }
        return commandHandler;
    }
}