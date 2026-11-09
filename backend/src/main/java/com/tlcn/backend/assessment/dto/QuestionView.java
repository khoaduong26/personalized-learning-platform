package com.tlcn.backend.assessment.dto;
import com.tlcn.backend.assessment.entity.Question;
public record QuestionView(Long id, String content, String difficulty, Double score, Long lessonId, String kind) {
    public static QuestionView from(Question q) {
        String kind = q.getClass().getSimpleName();
        return new QuestionView(q.getId(), q.getContent(), q.getDifficulty().name(), q.getScore(), q.getLesson().getId(), kind);
    }
}
