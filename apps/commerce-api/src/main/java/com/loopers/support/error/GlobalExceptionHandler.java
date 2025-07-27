package com.loopers.support.error;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CoreException.class)
    public ResponseEntity<Void> handleCoreException(CoreException e) {
        return ResponseEntity.status(e.getErrorType().getStatus()).build();
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Void> handleMissingRequestHeader(MissingRequestHeaderException e) {
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void> handleGenericException(Exception e) {
        return ResponseEntity.internalServerError().build();
    }
}