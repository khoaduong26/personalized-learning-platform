package com.tlcn.backend.academic.dto;

import com.tlcn.backend.academic.entity.Lesson;

public record LessonResponse(Long id, String name, String description, Long topicId, String topicName, Long subjectId) {
    public static LessonResponse from(Lesson lesson) {
        var topic = lesson.getTopic();
        return new LessonResponse(lesson.getId(), lesson.getName(), lesson.getDescription(), topic.getId(),
                topic.getName(), topic.getSubject().getId());
    }
}
