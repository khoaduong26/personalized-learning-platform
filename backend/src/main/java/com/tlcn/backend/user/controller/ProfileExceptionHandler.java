package com.tlcn.backend.user.controller;

import com.tlcn.backend.user.exception.ProfileException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice(assignableTypes = ProfileController.class)
public class ProfileExceptionHandler {
    public record ApiError(String code, String message, Map<String, String> fieldErrors) {}

    @ExceptionHandler(ProfileException.class)
    ResponseEntity<ApiError> profile(ProfileException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new ApiError(ex.getCode(), ex.getMessage(), ex.getFieldErrors()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", "Vui lòng kiểm tra thông tin hồ sơ.", fields));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestPartException.class})
    ResponseEntity<ApiError> invalidInput(Exception ex) {
        return ResponseEntity.badRequest().body(new ApiError("INVALID_REQUEST", "Dữ liệu không hợp lệ hoặc thiếu file ảnh.", Map.of()));
    }

}
