package com.tlcn.backend.academic.dto;
import com.tlcn.backend.academic.entity.Topic;
public record TopicResponse(Long id, String name, Long subjectId, String subjectName, int lessonCount) {
    public static TopicResponse from(Topic topic) { return new TopicResponse(topic.getId(), topic.getName(), topic.getSubject().getId(), topic.getSubject().getName(), topic.getLessons().size()); }
}
