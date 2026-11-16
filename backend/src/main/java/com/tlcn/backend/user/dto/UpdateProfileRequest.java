package com.tlcn.backend.user.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record UpdateProfileRequest(
        @NotBlank @Size(min = 2, max = 120) String fullName,
        @Pattern(regexp = "\\+?[0-9]{9,15}", message = "Số điện thoại phải gồm 9–15 chữ số, có thể bắt đầu bằng +.") String phone,
        @Size(max = 255) String address,
        @Size(max = 100) String city,
        @Pattern(regexp = "MALE|FEMALE|OTHER", message = "Giới tính phải là MALE, FEMALE hoặc OTHER.") String sex,
        @PastOrPresent(message = "Ngày sinh không được ở tương lai.") LocalDate dateOfBirth) {
    public UpdateProfileRequest {
        fullName = fullName == null ? null : fullName.strip();
        phone = optional(phone);
        address = optional(address);
        city = optional(city);
        sex = optional(sex);
    }
    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
