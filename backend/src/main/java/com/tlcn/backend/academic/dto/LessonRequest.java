package com.tlcn.backend.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LessonRequest(@NotBlank @Size(max = 160) String name, @Size(max = 5000) String description,
        @NotNull Long topicId) {
}
