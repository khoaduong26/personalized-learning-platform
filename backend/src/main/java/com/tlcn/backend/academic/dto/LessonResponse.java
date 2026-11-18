package com.tlcn.backend.academic.dto;

public record LessonResponse(Long id, String name, String description, Long topicId, String topicName, Long subjectId) {}
