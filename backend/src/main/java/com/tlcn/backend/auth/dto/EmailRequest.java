package com.tlcn.backend.auth.dto;

import jakarta.validation.constraints.*;
import java.util.Locale;

public record EmailRequest(@NotBlank @Email @Size(max = 190) String email) {
    public EmailRequest { email = email == null ? null : email.strip().toLowerCase(Locale.ROOT); }
}
