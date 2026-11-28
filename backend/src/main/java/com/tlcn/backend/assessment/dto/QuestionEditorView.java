package com.tlcn.backend.assessment.dto;

import com.tlcn.backend.assessment.entity.*;
import java.util.List;
import org.hibernate.Hibernate;

public record QuestionEditorView(Long id, String content, String difficulty, Double score, Long lessonId,
        String kind, boolean allowMultiple, boolean caseSensitive, String correctAnswer,
        List<OptionView> options, List<BlankView> blanks, String explanation) {
    public record OptionView(String name, boolean correct, Integer displayOrder) {}
    public record BlankView(String correctAnswer, Integer position) {}

    public static QuestionEditorView from(Question question) {
        question = (Question) Hibernate.unproxy(question);
        String kind = question instanceof MultipleChoiceQuestion ? "MULTIPLE_CHOICE"
                : question instanceof FillBlankQuestion ? "FILL_BLANK" : "SHORT_ANSWER";
        List<OptionView> options = question instanceof MultipleChoiceQuestion choice
                ? choice.getOptions().stream().map(option -> new OptionView(option.getName(), option.isCorrect(),
                        option.getDisplayOrder())).toList()
                : List.of();
        List<BlankView> blanks = question instanceof FillBlankQuestion fill
                ? fill.getBlanks().stream().map(blank -> new BlankView(blank.getCorrectAnswer(), blank.getPosition()))
                        .toList()
                : List.of();
        boolean caseSensitive = question instanceof FillBlankQuestion fill && fill.isCaseSensitive()
                || question instanceof ShortAnswerQuestion answer && answer.isCaseSensitive();
        String correctAnswer = question instanceof ShortAnswerQuestion answer ? answer.getCorrectAnswer() : "";
        return new QuestionEditorView(question.getId(), question.getContent(), question.getDifficulty().name(),
                question.getScore(), question.getLesson().getId(), kind,
                question instanceof MultipleChoiceQuestion choice && choice.isAllowMultiple(), caseSensitive,
                correctAnswer, options, blanks, question.getExplanation());
    }
}
