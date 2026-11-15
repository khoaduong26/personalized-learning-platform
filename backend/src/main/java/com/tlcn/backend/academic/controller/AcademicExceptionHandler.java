package com.tlcn.backend.academic.controller;
import com.tlcn.backend.academic.service.AcademicConflictException;
import com.tlcn.backend.academic.service.AcademicBadRequestException;
import com.tlcn.backend.academic.service.AcademicNotFoundException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
@RestControllerAdvice(basePackages = "com.tlcn.backend.academic")
public class AcademicExceptionHandler {
    @ExceptionHandler(AcademicBadRequestException.class)
    ResponseEntity<?> badRequest(AcademicBadRequestException ex) { return ResponseEntity.badRequest().body(Map.of("code", "ACADEMIC_INVALID_REQUEST", "message", ex.getMessage())); }
    @ExceptionHandler(AcademicNotFoundException.class)
    ResponseEntity<?> notFound(AcademicNotFoundException ex) { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("code", "ACADEMIC_NOT_FOUND", "message", ex.getMessage())); }
    @ExceptionHandler({AcademicConflictException.class, DataIntegrityViolationException.class})
    ResponseEntity<?> conflict(RuntimeException ex) { return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("code", "ACADEMIC_CONFLICT", "message", ex instanceof AcademicConflictException ? ex.getMessage() : "Dữ liệu đang được sử dụng hoặc đã tồn tại.")); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("code", "VALIDATION_ERROR", "message", "Vui lòng kiểm tra dữ liệu nhập.", "fieldErrors", fieldErrors));
    }
}
