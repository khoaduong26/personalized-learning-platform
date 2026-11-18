package com.tlcn.backend.assessment.dto;

import com.tlcn.backend.assessment.entity.*;
import java.util.List;

public record QuestionView(Long id, String content, String difficulty, Double score, Long lessonId, String kind,
                boolean allowMultiple, List<ChoiceView> options, int blankCount) {
        public record ChoiceView(Long id, String name, Integer displayOrder) {
        }

        public static QuestionView from(Question q) {
                String kind = q instanceof MultipleChoiceQuestion ? "MULTIPLE_CHOICE"
                                : q instanceof FillBlankQuestion ? "FILL_BLANK" : "SHORT_ANSWER";
                List<ChoiceView> options = q instanceof MultipleChoiceQuestion mc ? mc.getOptions().stream()
                                .map(option -> new ChoiceView(option.getId(), option.getName(),
                                                option.getDisplayOrder()))
                                .toList() : List.of();
                int blankCount = q instanceof FillBlankQuestion fill ? fill.getBlanks().size() : 0;
                return new QuestionView(q.getId(), q.getContent(), q.getDifficulty().name(), q.getScore(),
                                q.getLesson().getId(), kind,
                                q instanceof MultipleChoiceQuestion mc && mc.isAllowMultiple(), options, blankCount);
        }
}
