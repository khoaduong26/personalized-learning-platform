package com.tlcn.backend.classroom.controller;

import com.tlcn.backend.classroom.service.ClassroomConflictException;
import com.tlcn.backend.classroom.service.ClassroomNotFoundException;
import java.util.Map;
import java.util.HashMap;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.tlcn.backend.classroom")
public class ClassroomExceptionHandler {
    @ExceptionHandler(ClassroomNotFoundException.class)
    ResponseEntity<?> notFound(ClassroomNotFoundException error) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("code", "CLASSROOM_NOT_FOUND", "message", error.getMessage()));
    }

    @ExceptionHandler({ClassroomConflictException.class, DataIntegrityViolationException.class})
    ResponseEntity<?> conflict(RuntimeException error) {
        String message = error instanceof ClassroomConflictException ? error.getMessage() : "Dữ liệu đang được sử dụng hoặc bị trùng.";
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("code", "CLASSROOM_CONFLICT", "message", message));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> requestError(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("code", "INVALID_REQUEST", "message", error.getReason() == null ? "Yêu cầu không hợp lệ." : error.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validationError(MethodArgumentNotValidException error) {
        Map<String, String> fields = new HashMap<>();
        error.getBindingResult().getFieldErrors().forEach(field -> fields.putIfAbsent(field.getField(), field.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("code", "VALIDATION_ERROR", "message", "Vui lòng kiểm tra thông tin đã nhập.", "fieldErrors", fields));
    }
}
