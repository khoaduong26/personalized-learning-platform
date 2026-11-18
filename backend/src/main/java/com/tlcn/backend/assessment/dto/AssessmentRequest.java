package com.tlcn.backend.assessment.dto;

import com.tlcn.backend.assessment.entity.AssessmentType;
import jakarta.validation.constraints.*;
import java.util.List;

public record AssessmentRequest(
                @NotBlank @Size(max = 180) String title,
                @NotNull AssessmentType type,
                @Min(1) @Max(240) Integer durationMinutes,
                @NotEmpty List<@NotNull Long> questionIds,
                @NotEmpty List<@NotNull Long> classroomIds) {
}
