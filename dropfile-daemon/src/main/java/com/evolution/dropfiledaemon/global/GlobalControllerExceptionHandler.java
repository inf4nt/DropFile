package com.evolution.dropfiledaemon.global;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.ApiErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalControllerExceptionHandler {

    @ExceptionHandler({Exception.class})
    public ResponseEntity<?> exception(Exception exception, HttpServletRequest request) {
        log.error("Web controller error: {}", exception.getMessage(), exception);
        if (isApiCall(request)) {
            String stackTraceAsString = CommonUtils.getStackTraceAsString(exception);
            ApiErrorDTO apiErrorDTO = new ApiErrorDTO(exception.getClass().getName(), exception.getMessage(), stackTraceAsString);
            return ResponseEntity.badRequest()
                    .body(apiErrorDTO);
        }
        return ResponseEntity.notFound().build();
    }

    private boolean isApiCall(HttpServletRequest request) {
        return request != null && request.getServletPath() != null && request.getServletPath().startsWith("/api");
    }
}
