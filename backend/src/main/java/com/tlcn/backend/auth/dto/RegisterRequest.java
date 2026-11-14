package com.tlcn.backend.auth.dto;

import com.tlcn.backend.user.entity.Role;
import jakarta.validation.constraints.*;
import java.util.Locale;

public record RegisterRequest(
        @NotBlank @Size(min = 2, max = 120) String fullName,
        @NotBlank @Email @Size(max = 190) String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotBlank @Pattern(regexp = "\\+?[0-9]{9,15}", message = "Số điện thoại phải gồm 9–15 chữ số, có thể bắt đầu bằng +.") String phone,
        @NotNull Role role) {
    public RegisterRequest {
        fullName = fullName == null ? null : fullName.strip();
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
        phone = phone == null ? null : phone.strip();
    }
}
