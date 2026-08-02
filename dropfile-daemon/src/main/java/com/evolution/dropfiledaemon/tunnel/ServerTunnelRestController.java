package com.evolution.dropfiledaemon.tunnel;

import com.evolution.dropfile.common.CloseShieldOutputStream;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelDispatcher;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelDispatcherContext;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelRequestDTO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.WebAsyncTask;

import java.io.IOException;
import java.io.OutputStream;
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
                    __ -> tunnelDispatcher.dispatch(requestDTO)
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
            } catch (IOException e) {
                log.error("Failed to close tunnel context {}", e.getMessage(), e);
            }
        };

        webAsyncTask.onTimeout(() -> {
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
}
