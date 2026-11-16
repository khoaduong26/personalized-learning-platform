package com.tlcn.backend.auth.exception;

import org.springframework.http.HttpStatus;

// Failed attempts must commit even though verification returns a client error.
public class InvalidOtpException extends AuthException {
    public InvalidOtpException(String message) { super(HttpStatus.BAD_REQUEST, "OTP_INVALID", message); }
}
