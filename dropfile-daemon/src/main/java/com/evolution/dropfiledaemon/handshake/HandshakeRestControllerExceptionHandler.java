package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice(assignableTypes = HandshakeRestController.class)
public class HandshakeRestControllerExceptionHandler {

    @ExceptionHandler({Exception.class})
    public ResponseEntity<HandshakeResponseDTO> exception(Exception exception) {
        log.info("Handshake error: {}", exception.getMessage(), exception);
        byte[] payload = CommonUtils.nonce12();
        byte[] signature = CommonUtils.nonce12();
        return ResponseEntity.ok(
                new HandshakeResponseDTO(
                        payload,
                        CommonUtils.nonce12(),
                        signature
                )
        );
    }
}
