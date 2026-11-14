package com.tlcn.backend.auth.dto;

import jakarta.validation.constraints.*;
import java.util.Locale;

public record LoginRequest(@NotBlank @Email @Size(max = 190) String email,
        @NotBlank @Size(max = 128) String password, boolean rememberMe) {
    public LoginRequest {
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
