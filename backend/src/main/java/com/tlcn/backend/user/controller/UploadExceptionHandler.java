package com.tlcn.backend.user.controller;

import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

// Multipart limits can fail before Spring resolves a controller, so this advice is global.
@RestControllerAdvice
public class UploadExceptionHandler {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ProfileExceptionHandler.ApiError> tooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(
                new ProfileExceptionHandler.ApiError("AVATAR_TOO_LARGE", "Ảnh đại diện tối đa 2 MB.", Map.of()));
    }
}
