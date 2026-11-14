package com.tlcn.backend.academic.controller;
import com.tlcn.backend.academic.service.AcademicConflictException;
import com.tlcn.backend.academic.service.AcademicNotFoundException;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@RestControllerAdvice(basePackages = "com.tlcn.backend.academic")
public class AcademicExceptionHandler {
    @ExceptionHandler(AcademicNotFoundException.class)
    ResponseEntity<?> notFound(AcademicNotFoundException ex) { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("code", "ACADEMIC_NOT_FOUND", "message", ex.getMessage())); }
    @ExceptionHandler({AcademicConflictException.class, DataIntegrityViolationException.class})
    ResponseEntity<?> conflict(RuntimeException ex) { return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("code", "ACADEMIC_CONFLICT", "message", ex instanceof AcademicConflictException ? ex.getMessage() : "Dữ liệu đang được sử dụng hoặc đã tồn tại.")); }
}
