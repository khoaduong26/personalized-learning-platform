package com.tlcn.backend.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LessonImageRequest(@NotBlank @Size(max = 500) String url,
        @Size(max = 200) String altText, @Size(max = 300) String caption,
        @NotBlank String placement, @NotBlank String alignment, @NotBlank String displaySize) {}
