package com.evolution.dropfiledaemon.tunnel;

import com.evolution.dropfiledaemon.tunnel.framework.TunnelDispatcher;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelRequestDTO;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/tunnel")
public class TunnelRestController {

    private final TunnelDispatcher tunnelDispatcher;

    @Autowired
    public TunnelRestController(TunnelDispatcher tunnelDispatcher) {
        this.tunnelDispatcher = tunnelDispatcher;
    }

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
