package com.evolution.dropfiledaemon.tunnel;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Deprecated
@ControllerAdvice(assignableTypes = TunnelRestController.class)
public class TunnelRestControllerExceptionHandler {

    @ExceptionHandler({Exception.class})
    public ResponseEntity<StreamingResponseBody> exception(Exception e) {
        e.printStackTrace();
        return ResponseEntity.notFound().build();
    }

//    @ExceptionHandler({Exception.class})
//    public ResponseEntity<StreamingResponseBody> exception(Exception e) {
//        e.printStackTrace();
//        StreamingResponseBody stream = outputStream -> {
//            outputStream.write(CommonUtils.nonce12());
//            outputStream.write(CommonUtils.nonce16());
//        };
//        return ResponseEntity.ok()
//                .contentType(MediaType.APPLICATION_OCTET_STREAM)
//                .body(stream);
//    }
}
