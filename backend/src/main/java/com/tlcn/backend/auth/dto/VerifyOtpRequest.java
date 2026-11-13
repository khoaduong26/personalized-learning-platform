package com.tlcn.backend.auth.dto;

import jakarta.validation.constraints.*;
import java.util.Locale;

public record VerifyOtpRequest(
        @NotBlank @Email @Size(max = 190) String email,
        @NotBlank @Pattern(regexp = "[0-9]{6}", message = "Mã OTP gồm đúng 6 chữ số.") String otp) {
    public VerifyOtpRequest { email = email == null ? null : email.strip().toLowerCase(Locale.ROOT); }
}
