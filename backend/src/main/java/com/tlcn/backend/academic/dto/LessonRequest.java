package com.tlcn.backend.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import jakarta.validation.Valid;

public record LessonRequest(@NotBlank @Size(max = 160) String name, @Size(max = 5000) String description,
        @Size(max = 20000) String learningContent, @Valid @Size(max = 8) List<LessonImageRequest> images,
        @NotNull Long topicId) {
}
