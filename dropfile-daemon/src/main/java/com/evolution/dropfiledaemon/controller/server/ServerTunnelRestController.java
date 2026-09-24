package com.evolution.dropfiledaemon.controller.server;

import com.evolution.dropfile.common.io.CloseShieldOutputStream;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelDispatcher;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelDispatcherContext;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.WebAsyncTask;

import java.io.IOException;
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

    private final ObjectMapper objectMapper;

    @PostMapping(ServerTunnelRestController.TUNNEL_ENDPOINT)
    public WebAsyncTask<Void> stream(HttpServletRequest httpServletRequest,
                                     HttpServletResponse httpServletResponse) {

        AtomicReference<TunnelDispatcherContext> contextAtomicReference = new AtomicReference<>();

        WebAsyncTask<Void> webAsyncTask = new WebAsyncTask<>(() -> {
            TunnelRequestDTO requestDTO = deserialize(httpServletRequest);

            try (TunnelDispatcherContext tunnelDispatcherContext = contextAtomicReference.updateAndGet(
                    _ -> tunnelDispatcher.dispatch(requestDTO)
            )) {
                httpServletResponse.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                httpServletResponse.setStatus(HttpServletResponse.SC_OK);

                OutputStream outputStream = CloseShieldOutputStream.stream(httpServletResponse.getOutputStream());
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
            log.error("Tunnel dispatcher failed due to timeout. {}",
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
        String command = context.requestPayload().command();
        UUID requestId = context.requestPayload().requestId();
        long timestamp = context.requestPayload().timestamp();
        return "fingerprint '%s' command '%s' handshakeId '%s' timestamp '%s' instant '%s'".formatted(
                context.fingerprint(),
                command,
                requestId,
                timestamp,
                Instant.ofEpochMilli(timestamp)
        );
    }

    /**
     * Body is read inside WebAsyncTask on purpose (not via @RequestBody).
     *
     * Spring applies spring.mvc.async.request-timeout only after this method returns WebAsyncTask.
     * With @RequestBody, the container deserializes the request body before the method runs, so that
     * time is outside the async timeout. A slow client can drip the body for a long time and hold the
     * connection without the async timeout firing.
     *
     * Reading httpServletRequest.getInputStream() inside the callable folds body read + handling into
     * the same async timeout window. Keep an input size limit (filter/watchdog); do not "simplify"
     * back to @RequestBody without restoring a separate request-body read timeout.
     */
    private TunnelRequestDTO deserialize(HttpServletRequest request) throws IOException {
        return objectMapper.readValue(request.getInputStream(), TunnelRequestDTO.class);
    }
}
