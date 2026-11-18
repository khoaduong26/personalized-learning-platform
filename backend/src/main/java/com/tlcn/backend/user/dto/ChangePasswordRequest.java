package com.tlcn.backend.user.dto;

import jakarta.validation.constraints.*;

public record ChangePasswordRequest(
        @NotBlank @Size(max = 128) String currentPassword,
        @NotBlank @Size(min = 8, max = 128) String newPassword,
        @NotBlank @Size(min = 8, max = 128) String confirmPassword) {}
