package com.tlcn.backend.assessment.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.tlcn.backend.assessment")
public class AssessmentExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> requestError(ResponseStatusException error) {
        String code = error.getStatusCode().value() == 403 ? "FORBIDDEN"
                : error.getStatusCode().value() == 404 ? "NOT_FOUND"
                        : error.getStatusCode().value() == 409 ? "CONFLICT" : "INVALID_REQUEST";
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("code", code,
                "message", error.getReason() == null ? "Yêu cầu không hợp lệ." : error.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validationError(MethodArgumentNotValidException error) {
        return ResponseEntity.badRequest().body(Map.of("code", "VALIDATION_ERROR",
                "message", "Vui lòng kiểm tra thông tin đã nhập."));
    }
}
