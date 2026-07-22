package com.evolution.dropfiledaemon.tunnel;

import com.evolution.dropfiledaemon.tunnel.framework.server.TunnelServer;
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
@RequestMapping
public class TunnelServerRestController {

    public static final String TUNNEL_ENDPOINT = "public/tunnel";

    private final TunnelServer tunnelServer;

    @SneakyThrows
    @PostMapping(TunnelServerRestController.TUNNEL_ENDPOINT)
    public ResponseEntity<StreamingResponseBody> stream(@RequestBody TunnelRequestDTO requestDTO) {
        StreamingResponseBody stream = outputStream -> {
            tunnelServer.dispatchStream(requestDTO, outputStream);
        };
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(stream);
    }
}
