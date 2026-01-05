package com.evolution.dropfiledaemon.tunnel;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(assignableTypes = TunnelRestController.class)
public class TunnelRestControllerExceptionHandler {

    @ExceptionHandler({Exception.class})
    public ResponseEntity<?> exception(Exception e) {
        e.printStackTrace();
        return ResponseEntity.ok(
                new TunnelResponseDTO(
                        CommonUtils.encodeBase64(CommonUtils.nonce16()),
                        CommonUtils.encodeBase64(CommonUtils.nonce12())
                )
        );
    }
}
