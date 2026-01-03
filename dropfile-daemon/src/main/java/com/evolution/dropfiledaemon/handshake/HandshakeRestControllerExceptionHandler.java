package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.HandshakeResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(assignableTypes = HandshakeRestController.class)
public class HandshakeRestControllerExceptionHandler {

    @ExceptionHandler({Exception.class})
    public ResponseEntity<?> exception(Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(200).body(
                new HandshakeResponseDTO(
                        CommonUtils.encodeBase64(CommonUtils.nonce16()),
                        CommonUtils.encodeBase64(CommonUtils.nonce12())
                )
        );
    }
}
