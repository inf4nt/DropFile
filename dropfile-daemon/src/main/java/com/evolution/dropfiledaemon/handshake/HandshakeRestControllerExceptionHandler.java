package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoRSA;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeResponseDTO;
import com.evolution.dropfile.store.keys.KeysConfigStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice(assignableTypes = HandshakeRestController.class)
public class HandshakeRestControllerExceptionHandler {

    private final KeysConfigStore keysConfigStore;

    @Autowired
    public HandshakeRestControllerExceptionHandler(KeysConfigStore keysConfigStore) {
        this.keysConfigStore = keysConfigStore;
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<HandshakeResponseDTO> exception(Exception exception) {
        log.info("Handshake error: {}", exception.getMessage(), exception);
        byte[] payload = CommonUtils.nonce12();
        byte[] signature = CryptoRSA.sign(
                payload,
                CryptoRSA.getPrivateKey(keysConfigStore.getRequired().rsa().privateKey())
        );
        return ResponseEntity.ok(
                new HandshakeResponseDTO(
                        CommonUtils.encodeBase64(payload),
                        CommonUtils.encodeBase64(CommonUtils.nonce12()),
                        CommonUtils.encodeBase64(signature)
                )
        );
    }
}
