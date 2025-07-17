package com.project.freecruting.handler;


import com.project.freecruting.exception.ForbiddenException;
import com.project.freecruting.exception.InvalidStateException;
import com.project.freecruting.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        ErrorResponse errorResponse = new ErrorResponse();
        setErrorResponse(errorResponse, HttpStatus.BAD_REQUEST, ex);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse();
        setErrorResponse(errorResponse, HttpStatus.NOT_FOUND, ex);

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStateException(InvalidStateException ex) {
        ErrorResponse errorResponse = new ErrorResponse();
        setErrorResponse(errorResponse, HttpStatus.BAD_REQUEST, ex);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(ForbiddenException ex) {
        ErrorResponse errorResponse = new ErrorResponse();
        setErrorResponse(errorResponse, HttpStatus.FORBIDDEN, ex);

        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }



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

    private void setErrorResponse(ErrorResponse errorResponse, HttpStatus status, Exception ex) {
        errorResponse.setTimestamp(LocalDateTime.now());
        errorResponse.setStatus(status.value());
        errorResponse.setError(status.getReasonPhrase());
        errorResponse.setMessage("Error: " + ex.getMessage());
    }

}
