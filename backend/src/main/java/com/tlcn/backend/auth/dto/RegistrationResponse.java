package com.tlcn.backend.auth.dto;

import com.tlcn.backend.user.entity.UserStatus;
import java.time.Instant;

public record RegistrationResponse(String email, UserStatus status, Instant otpExpiresAt,
                                   Instant resendAvailableAt, String message) {}
