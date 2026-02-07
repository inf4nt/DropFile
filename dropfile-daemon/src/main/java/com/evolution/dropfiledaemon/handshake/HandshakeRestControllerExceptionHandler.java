package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoRSA;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeResponseDTO;
import com.evolution.dropfile.store.keys.KeysConfigStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(assignableTypes = HandshakeRestController.class)
public class HandshakeRestControllerExceptionHandler {

    private final KeysConfigStore keysConfigStore;

    @Autowired
    public HandshakeRestControllerExceptionHandler(KeysConfigStore keysConfigStore) {
        this.keysConfigStore = keysConfigStore;
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<?> exception(Exception e) {
        e.printStackTrace();
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
