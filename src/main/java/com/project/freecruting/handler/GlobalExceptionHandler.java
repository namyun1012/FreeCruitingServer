package com.project.freecruting.handler;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    
    // 임시용 Handler 추후에 예외 종류 늘려야 함
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleExceptions (Exception ex, WebRequest request) {
        logger.error("An unexpected error occurred: Request URI: {}", request.getDescription(false).replace("uri=", ""), ex);

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimestamp(LocalDateTime.now());
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        errorResponse.setStatus(status.value());
        errorResponse.setError(status.getReasonPhrase());

        errorResponse.setMessage("Error: " + ex.getMessage()); // 디버깅 목적으로만 사용하세요!

        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(errorResponse, status);
    }

}
