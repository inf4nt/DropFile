package com.evolution.dropfiledaemon.tunnel.framework.server.chain.procedure;

import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelDispatcherChainProcessorContext;
import com.evolution.dropfiledaemon.tunnel.framework.server.command.CommandHandlerExecutor;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelDispatcherChainProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Component
public class CommandTunnelDispatcherChain implements TunnelDispatcherChain {

    private final CommandHandlerExecutor commandHandlerExecutor;

    private final ObjectMapper objectMapper;

    @Override
    public void doChain(TunnelDispatcherChainProcessorContext ctx, TunnelDispatcherChainProcessor processor) throws IOException {
        Object result = commandHandlerExecutor.handle(ctx.tunnelRequestPayload());
        try (OutputStream outputStream = ctx.outputStream();
             InputStream inputStream = handlerResultToInputStream(result)) {
            inputStream.transferTo(outputStream);
            outputStream.flush();
        }
    }

    @SneakyThrows
    private InputStream handlerResultToInputStream(Object handlerResult) {
        if (handlerResult instanceof InputStream inputStream) {
            return inputStream;
        }
        if (handlerResult instanceof byte[] arrayResult) {
            return new ByteArrayInputStream(arrayResult);
        }
        if (handlerResult instanceof String stringResult) {
            return new ByteArrayInputStream(stringResult.getBytes());
        }

        byte[] bytes = objectMapper.writeValueAsBytes(handlerResult);
        return new ByteArrayInputStream(bytes);
    }
}
