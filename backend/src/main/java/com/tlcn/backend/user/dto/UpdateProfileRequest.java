package com.tlcn.backend.user.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record UpdateProfileRequest(
        @NotBlank @Size(min = 2, max = 120) String fullName,
        @Pattern(regexp = "[0-9]{10}", message = "Số điện thoại phải gồm đúng 10 chữ số, không có dấu hoặc ký tự khác.") String phone,
        @Size(max = 255) String address,
        @Size(max = 100) String city,
        // OTHER may only be retained on an existing profile; the service rejects new selections.
        @Pattern(regexp = "MALE|FEMALE|OTHER", message = "Vui lòng chọn giới tính Nam hoặc Nữ.") String sex,
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
