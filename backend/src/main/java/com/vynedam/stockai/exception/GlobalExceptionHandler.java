package com.vynedam.stockai.exception;

import java.util.LinkedHashMap;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    record Error(String code, String message, Object details) {}
    record Body(Error error) {}

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Body> api(ApiException exception) {
        return ResponseEntity.status(exception.status)
                .body(new Body(new Error(exception.code, exception.getMessage(), null)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Body> validation(MethodArgumentNotValidException exception) {
        var details = new LinkedHashMap<String, String>();
        exception.getBindingResult().getFieldErrors().forEach(error -> details.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new Body(new Error("VALIDATION_ERROR", "Request validation failed", details)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Body> malformedRequest() {
        return ResponseEntity.badRequest().body(new Body(new Error("VALIDATION_ERROR", "Malformed JSON or unsupported field value", null)));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    ResponseEntity<Body> duplicate() {
        return ResponseEntity.status(409).body(new Body(new Error("DUPLICATE_RECORD", "A unique value already exists", null)));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Body> denied() {
        return ResponseEntity.status(403).body(new Body(new Error("FORBIDDEN", "Insufficient permission", null)));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Body> unexpected() {
        return ResponseEntity.internalServerError().body(new Body(new Error("INTERNAL_ERROR", "An unexpected error occurred", null)));
    }
}
