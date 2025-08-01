package com.loopers.support.error;

import com.loopers.interfaces.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CoreException.class)
    public ResponseEntity<ApiResponse<Object>> handleCoreException(CoreException e) {
        ApiResponse<Object> response = ApiResponse.fail(e.getErrorType().getCode(), e.getErrorType().getMessage());
        return new ResponseEntity<>(response, e.getErrorType().getStatus());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingRequestHeader(MissingRequestHeaderException e) {
        ApiResponse<Object> response = ApiResponse.fail("MISSING_HEADER", "Required header is missing");
        return new ResponseEntity<>(response, org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        if (e.getMessage().contains("not found") || e.getMessage().contains("Not found")) {
            ApiResponse<Object> response = ApiResponse.fail("NOT_FOUND", e.getMessage());
            return new ResponseEntity<>(response, org.springframework.http.HttpStatus.NOT_FOUND);
        }
        ApiResponse<Object> response = ApiResponse.fail("BAD_REQUEST", e.getMessage());
        return new ResponseEntity<>(response, org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception e) {
        ApiResponse<Object> response = ApiResponse.fail("INTERNAL_ERROR", "An unexpected error occurred");
        return new ResponseEntity<>(response, org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
    }
}