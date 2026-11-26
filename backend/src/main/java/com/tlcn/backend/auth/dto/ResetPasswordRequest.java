package com.tlcn.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String resetToken,
        @NotBlank @Size(min = 8, max = 128, message = "Mật khẩu phải có từ 8 đến 128 ký tự.") String newPassword,
        @NotBlank @Size(max = 128) String confirmPassword) {
    @Override
    public String toString() {
        return "ResetPasswordRequest[redacted]";
    }
}
