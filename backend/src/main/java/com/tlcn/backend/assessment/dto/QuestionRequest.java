package com.tlcn.backend.assessment.dto;

import com.tlcn.backend.assessment.entity.Difficulty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record QuestionRequest(
                @NotBlank @Size(max = 4000) String content,
                @NotNull Difficulty difficulty,
                @NotNull @DecimalMin("0.1") @DecimalMax("100") Double score,
                @NotNull Long lessonId,
                @NotBlank String kind,
                Boolean allowMultiple,
                Boolean caseSensitive,
                String correctAnswer,
                @Valid List<OptionInput> options,
                @Valid List<BlankInput> blanks,
                String explanation) {
        public record OptionInput(@NotBlank @Size(max = 1000) String name, boolean correct,
                        @NotNull @Min(0) Integer displayOrder) {
        }

        public record BlankInput(@NotBlank @Size(max = 500) String correctAnswer,
                        @NotNull @Min(0) Integer position) {
        }
}
