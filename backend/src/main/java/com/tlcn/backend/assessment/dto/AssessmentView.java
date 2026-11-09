package com.tlcn.backend.assessment.dto;
import com.tlcn.backend.assessment.entity.Assessment;
import java.util.List;
public record AssessmentView(Long id, String title, String type, Integer durationMinutes, List<QuestionView> questions) {
    public static AssessmentView from(Assessment a) { return new AssessmentView(a.getId(),a.getTitle(),a.getType().name(),a.getDurationMinutes(),a.getQuestions().stream().map(QuestionView::from).toList()); }
}
