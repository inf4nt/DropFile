package com.evolution.dropfiledaemon.controller.server;

import com.evolution.dropfile.common.io.CloseShieldOutputStream;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelDispatcher;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelDispatcherContext;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelRequestDTO;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.WebAsyncTask;

import java.io.OutputStream;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping
public class ServerTunnelRestController {

    public static final String TUNNEL_ENDPOINT = "/s/t";

    private final TunnelDispatcher tunnelDispatcher;

    private final DaemonApplicationProperties applicationProperties;

    @PostMapping(ServerTunnelRestController.TUNNEL_ENDPOINT)
    public WebAsyncTask<Void> stream(@RequestBody TunnelRequestDTO requestDTO,
                                     HttpServletResponse response) {

        AtomicReference<TunnelDispatcherContext> contextAtomicReference = new AtomicReference<>();

        WebAsyncTask<Void> webAsyncTask = new WebAsyncTask<>(applicationProperties.daemonTunnelServerAsyncRequestTimeout, () -> {
            try (TunnelDispatcherContext tunnelDispatcherContext = contextAtomicReference.updateAndGet(
                    _ -> tunnelDispatcher.dispatch(requestDTO)
            )) {
                response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                response.setStatus(HttpServletResponse.SC_OK);

                OutputStream outputStream = CloseShieldOutputStream.stream(response.getOutputStream());
                tunnelDispatcher.transfer(tunnelDispatcherContext, outputStream);
                outputStream.flush();
            }
            return null;
        });

        Runnable safeClose = () -> {
            try {
                TunnelDispatcherContext tunnelDispatcherContext = contextAtomicReference.get();
                if (tunnelDispatcherContext != null) {
                    tunnelDispatcherContext.close();
                }
            } catch (Throwable throwable) {
                log.error("Failed to close tunnel context {}", throwable.getMessage(), throwable);
            }
        };

        webAsyncTask.onTimeout(() -> {
            String message = extractMessage(contextAtomicReference.get());
            log.error("Tunnel dispatcher failed due to timeout. Timeout '{}' fingerprint '{}' {}",
                    applicationProperties.daemonTunnelServerAsyncRequestTimeout,
                    requestDTO.fingerprint(),
                    message
            );
            safeClose.run();
            return null;
        });

        webAsyncTask.onError(() -> {
            safeClose.run();
            return null;
        });

        webAsyncTask.onCompletion(safeClose);

        return webAsyncTask;
    }

    private String extractMessage(@Nullable TunnelDispatcherContext context) {
        if (context == null) {
            return "";
        }
        String command = context.getRequestPayload().command();
        UUID requestId = context.getRequestPayload().requestId();
        long timestamp = context.getRequestPayload().timestamp();
        return "command '%s' requestId '%s' timestamp '%s' instant '%s'".formatted(
                command,
                requestId,
                timestamp,
                Instant.ofEpochMilli(timestamp)
        );
    }
}
