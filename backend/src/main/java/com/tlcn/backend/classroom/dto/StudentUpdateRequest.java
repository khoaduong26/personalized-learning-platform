package com.tlcn.backend.classroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record StudentUpdateRequest(
                @NotBlank @Size(max = 120) String fullName,
                @Size(max = 30) String sex,
                @Past LocalDate dateOfBirth) {
}
