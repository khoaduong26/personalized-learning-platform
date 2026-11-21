package com.tlcn.backend.user.dto;

import com.tlcn.backend.user.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public final class AccountRequests {
    private static final String PHONE_PATTERN = "\\+?[0-9]{9,15}";

    private AccountRequests() {}

    public record Create(
            @NotBlank @Size(min = 2, max = 120) String fullName,
            @NotBlank @Email @Size(max = 190) String email,
            @Pattern(regexp = PHONE_PATTERN) String phone,
            @NotNull Role role,
            @NotBlank @Size(min = 8, max = 128) String password) {
        public Create {
            fullName = strip(fullName);
            email = normalizeEmail(email);
            phone = normalizePhone(phone);
        }
    }

    public record Update(
            @NotBlank @Size(min = 2, max = 120) String fullName,
            @NotBlank @Email @Size(max = 190) String email,
            @Pattern(regexp = PHONE_PATTERN) String phone,
            @NotNull Role role) {
        public Update {
            fullName = strip(fullName);
            email = normalizeEmail(email);
            phone = normalizePhone(phone);
        }
    }

    public record Lock(@NotNull Boolean locked) {}

    private static String strip(String value) {
        return value == null ? null : value.strip();
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    private static String normalizePhone(String phone) {
        return phone == null || phone.isBlank() ? null : phone.strip();
    }
}
