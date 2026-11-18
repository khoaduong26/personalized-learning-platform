package com.tlcn.backend.assessment.dto;

import com.tlcn.backend.assessment.entity.Assessment;
import java.util.List;

public record AssessmentView(Long id, String title, String type, Integer durationMinutes,
        boolean published, Long teacherId, String teacherName, java.time.Instant createdAt,
        List<Long> classroomIds, List<String> classroomNames, List<QuestionView> questions) {
    public static AssessmentView from(Assessment a) {
        return new AssessmentView(a.getId(), a.getTitle(), a.getType().name(), a.getDurationMinutes(),
                a.isPublished(), a.getTeacher().getId(), a.getTeacher().getFullName(), a.getCreatedAt(),
                a.getClassrooms().stream().map(c -> c.getId()).toList(),
                a.getClassrooms().stream().map(c -> c.getName()).toList(),
                a.getQuestions().stream().map(QuestionView::from).toList());
    }
}
