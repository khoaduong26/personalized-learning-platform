package com.tlcn.backend.assessment.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AnswerRequest(@NotNull Long questionId, List<Long> selectedOptionIds,
                List<String> blankAnswers, String content) {
}
