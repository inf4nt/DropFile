package com.evolution.dropfiledaemon.tunnel;

import com.evolution.dropfiledaemon.tunnel.framework.TunnelDispatcher;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelRequestDTO;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelResponseDTO;
import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/tunnel")
public class TunnelRestController {

    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    private final TunnelDispatcher tunnelDispatcher;

    public TunnelRestController(TunnelDispatcher tunnelDispatcher) {
        this.tunnelDispatcher = tunnelDispatcher;
    }

    @SneakyThrows
    @PostMapping
    public TunnelResponseDTO tunnel(@RequestBody TunnelRequestDTO requestDTO) {
        return tunnelDispatcher.dispatch(requestDTO);
    }

    @SneakyThrows
    @PostMapping("/stream")
    public ResponseEntity<StreamingResponseBody> tunnelStream(@RequestBody TunnelRequestDTO requestDTO) {
        StreamingResponseBody stream = outputStream -> {
            tunnelDispatcher.dispatchStream(requestDTO, outputStream);
        };
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(stream);
    }
}
