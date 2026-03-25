package com.evolution.dropfiledaemon.tunnel;

import com.evolution.dropfiledaemon.tunnel.framework.TunnelDispatcher;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RequiredArgsConstructor
@RestController
@RequestMapping(TunnelRestController.TUNNEL_ENDPOINT)
public class TunnelRestController {

    public static final String TUNNEL_ENDPOINT = "public/tunnel";

    private final TunnelDispatcher tunnelDispatcher;

    @SneakyThrows
    @PostMapping
    public ResponseEntity<StreamingResponseBody> stream(@RequestBody TunnelRequestDTO requestDTO) {
        StreamingResponseBody stream = outputStream -> {
            tunnelDispatcher.dispatchStream(requestDTO, outputStream);
        };
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(stream);
    }
}
