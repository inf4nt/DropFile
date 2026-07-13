package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.ApiErrorDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice(assignableTypes = {
        ApiConnectionsAccessRestController.class,
        ApiConnectionsBrowseRestController.class,
        ApiConnectionsRestController.class,
        ApiDaemonRestController.class,
        ApiConnectionsShareRestController.class,
        ApiDownloadRestController.class,
        ApiQuickShareRestController.class
})
public class ApiControllerExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDTO> exception(Exception e) {
        log.info("Api call exception. Message: {}", e.getMessage(), e);
        String stackTraceAsString = CommonUtils.getStackTraceAsString(e);
        ApiErrorDTO apiErrorDTO = new ApiErrorDTO(e.getClass().getName(), e.getMessage(), stackTraceAsString);
        return ResponseEntity.badRequest()
                .body(apiErrorDTO);
    }
}
