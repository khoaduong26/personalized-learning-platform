package com.tlcn.backend.user.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class ProfileException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final Map<String, String> fieldErrors;

    public ProfileException(HttpStatus status, String code, String message) {
        this(status, code, message, Map.of());
    }
    public ProfileException(HttpStatus status, String code, String message, Map<String, String> fieldErrors) {
        super(message);
        this.status = status;
        this.code = code;
        this.fieldErrors = Map.copyOf(fieldErrors);
    }
    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public Map<String, String> getFieldErrors() { return fieldErrors; }
}
