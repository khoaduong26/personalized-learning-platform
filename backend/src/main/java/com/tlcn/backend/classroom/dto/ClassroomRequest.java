package com.tlcn.backend.classroom.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
public record ClassroomRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Pattern(regexp = "^\\d{4}-\\d{4}$", message = "Năm học cần theo dạng YYYY-YYYY.") String schoolYear,
        Long teacherId) {}
